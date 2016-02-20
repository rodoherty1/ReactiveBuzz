package io.rob

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.LazyLogging

object Main extends App with LazyLogging {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  val gitClient = system.actorOf(Props[GitClient], "GitClient")
  val twitterClient = system.actorOf(Props[TwitterClient], "TwitterClient")
  val reporter = system.actorOf(Props(classOf[Reporter], gitClient, twitterClient), "Reporter")

  logger.info("Checking out the buzz about #Reactive applications")
  reporter ! FetchReactiveBuzz
}

