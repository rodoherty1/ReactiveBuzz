package io.rob

import akka.actor.{ActorLogging, Props, Actor}
import io.rob.CommonDefs.FetchReactiveBuzz
import io.rob.GitClient.ReactiveProjects

class Reporter extends Actor with ActorLogging {

  val controller = context.actorOf(Props[Controller], "Controller")

  override def receive: Receive = {
    case FetchReactiveBuzz => controller ! FetchReactiveBuzz
    case projects@ReactiveProjects(names) =>
      log.info(names.mkString("{", ", ", "}"))
      context.system.terminate()
  }
}
