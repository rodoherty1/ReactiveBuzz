package io.rob

import akka.actor.{ActorLogging, Props, Actor}
import io.rob.GitClient.{UnableToGetReactiveProjects, ErrorRetrievingReactiveProjects, ReactiveProjects}


class Controller extends Actor with ActorLogging {

  def createGitClient() = context.actorOf(Props[GitClient], "GitClient")

  val gitClient = createGitClient()

  override def receive: Receive = {
    case _ => ()
//    case FetchReactiveBuzz => gitClient ! GetReactiveProjects
//
//    case projects@ReactiveProjects(names) =>
//      log.info(names.mkString("{", ", ", "}"))
//      context.parent ! projects
//    case UnableToGetReactiveProjects(msg) => log.warning(msg)
//    case ErrorRetrievingReactiveProjects(th) => log.error(th, th.getMessage)
  }

  def shutdown(): Unit = {
    // IO(Http).ask(Http.CloseAll)(1.second).await
    context.stop(self)
  }

}
