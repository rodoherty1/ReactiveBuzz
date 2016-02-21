package io.rob

import java.util.UUID

import akka.actor._

import com.fasterxml.jackson.databind.{SerializationFeature, DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import io.rob.GitClient.ReactiveProjects
import org.json4s.{DefaultFormats, jackson}

object Reporter {
  case class ReportItem(projectName: String, tweets: Seq[String])

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.enable(SerializationFeature.INDENT_OUTPUT)
}

class Reporter(gitClient: ActorRef, twitterClient: ActorRef) extends Actor with ActorLogging {

  import Reporter._

  /*
   * Values required for writing the final Report as JSON
   */
  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  type Report = (Int, List[ReportItem])

  // Keeps track of all reports which are still in progress
  def uuid = java.util.UUID.randomUUID

  // Stores individual report items as they are returned from Twitter.
  // When all reportItems have been collected, the report is printed out.
  var pendingReports: Map[UUID, Report] = Map.empty


  override def receive: Receive = {
    case FetchReactiveBuzz => gitClient ! GetReactiveProjects(uuid)

    case ReactiveProjects(id, names) =>
      pendingReports = pendingReports.updated(id, (names.size, List.empty))
      names.foreach(twitterClient ! GetTweets(id, _))

    case TwitterResult(id, reactiveProject, tweets) =>
      if (pendingReports.contains(id)) {
        val (count, reportItems) = pendingReports(id)

        val updatedResults = ReportItem(reactiveProject, tweets.map(_.text)) :: reportItems

        pendingReports = pendingReports updated (id, (count, updatedResults))
        if (count == updatedResults.size) {
          self ! PrintReport(id)
        }
      }

    case PrintReport(id) =>
      if (pendingReports.contains(id)) {
        pendingReports(id) match {
          case (_, results) => println(mapper.writeValueAsString(results))
        }
        pendingReports = pendingReports - id
      }
      context.parent ! Finished

    case Error(msg) =>
      log.error(msg)
      context.parent ! Finished
  }
}
