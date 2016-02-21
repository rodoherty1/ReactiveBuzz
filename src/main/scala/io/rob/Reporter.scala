package io.rob

import java.util.UUID

import akka.actor._
import io.rob.GitClient.ReactiveProjects

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
        val updatedResults = s"$reactiveProject -> ${asString(tweet)}" :: results

        pendingReports = pendingReports updated (id, (count, updatedResults))
        if (count == updatedResults.size) {
          self ! PrintReport(id)
        }
      }

    case PrintReport(id) =>
      log.info ("""All result received.  Here's your report on github projects and tweets relating to the term "Reactive"!""")
      if (pendingReports.contains(id)) {
        pendingReports(id) match {
          case (_, results) => println(results.mkString("\n"))
        }
        pendingReports = pendingReports - id
      }

    case Error(msg) =>
      log.error(msg)
      self ! Finished

    case Finished =>
      gitClient ! PoisonPill
      twitterClient ! PoisonPill
      self ! PoisonPill
  }

  def asString(maybeTweet: Option[Tweet]): String = maybeTweet match {
    case None => "[ No tweets found ]"
    case Some(tweet) => s"""{${tweet.text}}"""
  }
}
