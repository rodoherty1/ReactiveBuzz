package io.rob

import java.util.UUID

import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import io.rob.GitClient.{ErrorRetrievingReactiveProjects, UnableToGetReactiveProjects, ReactiveProjects}

class Reporter(gitClient: ActorRef, twitterClient: ActorRef) extends Actor with ActorLogging {

  type Report = (Int, List[String])

  def uuid = java.util.UUID.randomUUID

  var pendingReports: Map[UUID, Report] = Map.empty

  override def receive: Receive = {
    case FetchReactiveBuzz => gitClient ! GetReactiveProjects(uuid)

    case ReactiveProjects(id, names) =>
      pendingReports = pendingReports.updated(id, (names.size, List.empty))
      names.foreach(twitterClient ! GetTweets(id, _))

    case TwitterResult(id, reactiveProject, tweet) =>
      if (pendingReports.contains(id)) {
        val (count, results) = pendingReports(id)
        val updatedResults = s"$reactiveProject -> [$tweet]" :: results

        pendingReports = pendingReports updated (id, (count, updatedResults))
        if (count == updatedResults.size) {
          self ! PrintReport(id)
        }
      }

    case PrintReport(id) =>
      if (pendingReports.contains(id)) {
        pendingReports(id) match {
          case (_, results) => println(results.mkString("\n"))
        }
        pendingReports = pendingReports - id
      }

    case UnableToGetReactiveProjects(msg) => log.warning(msg)

    case ErrorRetrievingReactiveProjects(th) => log.error(th, th.getMessage)

    case FailedToAuthenticate => log.warning(FailedToAuthenticate.toString)
  }

  def shutdown(): Unit = {
    // IO(Http).ask(Http.CloseAll)(1.second).await
    context.stop(self)
  }


}
