package io.rob

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.util._

import scala.concurrent.duration._
import scala.util.{Failure, Success}


object GitClient {
  case class GitProject(name: String)
  case class GitResult(total_count: Int, incomplete_results: Boolean, items: List[GitProject])
  case class ReactiveProjects(names: List[String])
  case class UnableToGetReactiveProjects(msg: String)
  case class ErrorRetrievingReactiveProjects(th: Throwable)

  object GitJsonProtocol extends DefaultJsonProtocol {
    implicit def gitProjectFormat = jsonFormat1(GitProject)
    implicit def gitResultFormat = jsonFormat3(GitResult)
  }

}
class GitClient extends Actor with ActorLogging {

  import GitClient._
  import GitClient.GitJsonProtocol._
  import SprayJsonSupport._

  implicit val system = context.system
  import system.dispatcher

  override def receive: Receive = {
    case Controller.GetReactiveProjects => context.become(waitForResults(sender()))
      fetchReactiveProjects()
  }

  def waitForResults(controller: ActorRef): Actor.Receive = {
    case Controller.GetReactiveProjects => log.info("Ignoring subsequent requests for #Reactive projects")

    case results@ReactiveProjects => controller ! results; context.unbecome()
    case noResults@UnableToGetReactiveProjects => noResults; context.unbecome()
    case error@ErrorRetrievingReactiveProjects => error; context.unbecome()
  }


  def fetchReactiveProjects(): Unit = {
    log.info("Getting #Reactive projects from GitHub and their respective tweets")

    val pipeline = sendReceive ~> unmarshal[GitResult]

    val responseFuture = pipeline {
      Get("https://api.github.com/search/repositories?q=reactive")
    }

    responseFuture onComplete {
      case Success(GitResult(count, _, items)) =>
        self ! ReactiveProjects(items.take(10).map(_.name))

      case Success(somethingUnexpected) =>
        self ! UnableToGetReactiveProjects(s"The Git API call was successful but returned something unexpected: '$somethingUnexpected'.")

      case Failure(error) =>
        self ! ErrorRetrievingReactiveProjects(error)
    }
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    context.stop(self)
  }

}
