package io.rob

import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import io.rob.CommonDefs.{GetReactiveProjects, FetchReactiveBuzz}
import io.rob.GitClient.{ErrorRetrievingReactiveProjects, UnableToGetReactiveProjects, ReactiveProjects}

class Reporter(gitClient: ActorRef, twitterClient: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case FetchReactiveBuzz => gitClient ! GetReactiveProjects

    case projects@ReactiveProjects(names) =>
      names.foreach(twitterClient ! GetTweets(_))

    case UnableToGetReactiveProjects(msg) => log.warning(msg)

    case ErrorRetrievingReactiveProjects(th) => log.error(th, th.getMessage)

    case FailedToAuthenticate => log.warning(FailedToAuthenticate.toString)
  }

  def shutdown(): Unit = {
    // IO(Http).ask(Http.CloseAll)(1.second).await
    context.stop(self)
  }


}
