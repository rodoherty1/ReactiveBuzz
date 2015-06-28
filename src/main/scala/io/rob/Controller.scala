package io.rob

import akka.actor.{ActorLogging, Props, Actor}
import io.rob.Controller.GetReactiveProjects
import io.rob.GitClient.{UnableToGetReactiveProjects, ErrorRetrievingReactiveProjects, ReactiveProjects}

object Controller {
  case object GetReactiveProjects
}

class Controller extends Actor with ActorLogging {

  def createGitClient() = context.actorOf(Props[GitClient], "GitClient")

  val gitClient = createGitClient()

  override def receive: Receive = {
    case Receptionist.FetchReactiveBuzz => gitClient ! GetReactiveProjects

    case ReactiveProjects(names) => log.info(names.mkString("{", ", ", "}"))
    case UnableToGetReactiveProjects(msg) => log.warning(msg)
    case ErrorRetrievingReactiveProjects(th) => log.error(th, th.getMessage)
  }

  def shutdown(): Unit = {
    // IO(Http).ask(Http.CloseAll)(1.second).await
    context.stop(self)
  }

}
