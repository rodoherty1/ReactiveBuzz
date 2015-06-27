package io.rob

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem}
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.util._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class Main extends Actor {

  case class GitProject(name: String)
  case class GitResult(total_count: Int, incomplete_results: Boolean, items: List[GitProject])

  object GitJsonProtocol extends DefaultJsonProtocol {
    implicit def gitProjectFormat = jsonFormat1(GitProject)
    implicit def gitResultFormat = jsonFormat3(GitResult)
  }

  import system.dispatcher
  import GitJsonProtocol._
  import SprayJsonSupport._

  implicit val system = ActorSystem("MyActorSystem")

  val log = Logging(system, getClass)

  log.info("Getting #Reactive projects from GitHub and their respective tweets")

  val pipeline = sendReceive ~> unmarshal[GitResult]

  val responseFuture = pipeline {
    Get("https://api.github.com/search/repositories?q=reactive")
  }

  responseFuture onComplete {
    case Success(GitResult(count, _, items)) =>
      val itemsAsStr = items.take(10).map(_.name).mkString("{", ",", "}")
      log.info(s"Received $count results: $itemsAsStr")

      shutdown()

    case Success(somethingUnexpected) =>
      log.warning("The Git API call was successful but returned something unexpected: '{}'.", somethingUnexpected)
      shutdown()

    case Failure(error) =>
      log.error(error, "The Git API call failed")
      shutdown()
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }

  override def receive: Receive = {
    case _ => ()
  }

}

