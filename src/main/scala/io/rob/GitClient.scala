package io.rob

import java.util.UUID

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GitClient {
  case class GitProject(name: String)
  case class GitResult(total_count: Int, incomplete_results: Boolean, items: List[GitProject])
  case class ReactiveProjects(uuid: UUID, names: List[String])
}

class GitClient extends Actor with ActorLogging {

  import GitClient._
  import org.json4s._
  import org.json4s.jackson.JsonMethods._

  implicit val formats = DefaultFormats

  implicit val dispatcher = context.system.dispatcher
  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  override def receive: Receive = {
    case GetReactiveProjects(id) =>
      context.become(waitForResults(sender()))
      fetchReactiveProjects(id)
  }

  def waitForResults(reporter: ActorRef): Actor.Receive = {
    case GetReactiveProjects => log.info("Ignoring subsequent requests for #Reactive projects")

    case results@ReactiveProjects(_, _) =>
      reporter ! results
      context.unbecome()

    case err@Error(msg) =>
      reporter ! err
      context.unbecome()
  }


  def fetchReactiveProjects(id: UUID): Unit = {

    fetchReactiveProjectsFromGit() onComplete {
      case Success(GitResult(count, _, items)) =>
        val projects = items.take(10).map(_.name)
        log.info("Received #Reactive projects from GitHub: {}", projects)
        self ! ReactiveProjects(id, projects)

      case Success(somethingUnexpected) =>
        self ! Error(s"The Git API call was successful but returned something unexpected: '$somethingUnexpected'.")

      case Failure(error) =>
        self ! Error(error.getMessage)
    }
  }

  def fetchReactiveProjectsFromGit(): Future[GitResult] = {
    val future = Http().singleRequest(HttpRequest(uri = "https://api.github.com/search/repositories?q=reactive"))

    for {
      response <- future
      byteString <- response.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield parse(byteString.decodeString("UTF-8")).extract[GitResult]
  }

  override def postStop() = {
    materializer.shutdown()
  }
}
