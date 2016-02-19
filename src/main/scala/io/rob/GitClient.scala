package io.rob

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.stream.scaladsl.ImplicitMaterializer
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GitClient {
  case class GitProject(name: String)
  case class GitResult(total_count: Int, incomplete_results: Boolean, items: List[GitProject])

  case class ReactiveProjects(names: List[String])
  case class UnableToGetReactiveProjects(msg: String)
  case class ErrorRetrievingReactiveProjects(th: Throwable)
}

class GitClient extends Actor with ImplicitMaterializer with ActorLogging {

  import GitClient._
  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  import io.rob.CommonDefs.GetReactiveProjects

  implicit val formats = DefaultFormats

  implicit val system = context.system

  import system.dispatcher

  import akka.pattern.pipe
  import context.dispatcher

  implicit val executionContext = system.dispatchers.defaultGlobalDispatcher


  val http = Http(context.system)

  override def receive: Receive = {
    case GetReactiveProjects =>
      log.info("Fetching info from Github")
      context.become(waitForResults(sender()))
      fetchReactiveProjects()
    case _ => log.warning("Unexpected message received!")
  }

  def waitForResults(sender: ActorRef): Actor.Receive = {
    case GetReactiveProjects => log.info("Ignoring subsequent requests for #Reactive projects")

    case results@ReactiveProjects(projects) =>
      log.info("Received #Reactive projects from GitHub and their respective tweets")
      sender ! results
      context.unbecome()

    case noResults@UnableToGetReactiveProjects(msg) =>
      log.warning(noResults.msg)
      sender ! noResults
      context.unbecome()

    case error@ErrorRetrievingReactiveProjects(th) =>
      log.error(th.getMessage, th)
      sender ! error
      context.unbecome()

    case x@_ => log.warning("Unexpected message: {}", x)
  }


  def fetchReactiveProjects(): Unit = {
    log.info("Getting #Reactive projects from GitHub and their respective tweets")

    val responseFuture = fetchReactiveProjectsFromGit()

    responseFuture onComplete {
      case Success(GitResult(count, _, items)) =>
        val projects: List[String] = items.take(10).map(_.name)
        log.info("Received #Reactive projects from GitHub: {}", projects)
        self ! ReactiveProjects(projects)

      case Success(somethingUnexpected) =>
        self ! UnableToGetReactiveProjects(s"The Git API call was successful but returned something unexpected: '$somethingUnexpected'.")

      case Failure(error) =>
        self ! ErrorRetrievingReactiveProjects(error)
    }
  }


  def get(responseFuture: Future[HttpResponse]) = {
    log.info("Got response {}", responseFuture)
    responseFuture
  }

  def fetchReactiveProjectsFromGit(): Future[GitResult] = {
    val responseFuture =
      Http().singleRequest(HttpRequest(uri = "https://api.github.com/search/repositories?q=reactive"))

    for {
      response <- get(responseFuture)
      byteString <- response.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield parse(byteString.decodeString("UTF-8")).extract[GitResult]
  }


  def shutdown(): Unit = {
//    IO(Http).ask(Http.CloseAll)(1.second).await
    context.stop(self)
  }
}
