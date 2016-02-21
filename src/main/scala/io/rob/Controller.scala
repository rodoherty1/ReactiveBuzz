package io.rob

import akka.actor._

object Controller {
  case object Start
}

/**
  * This actor starts the process of requesting projects and tweets
  */
class Controller extends Actor with ActorLogging {

  import Controller._

  val gitClient = context.actorOf(Props[GitClient], "GitClient")
  val twitterClient = context.actorOf(Props[TwitterClient], "TwitterClient")
  val reporter = context.actorOf(Props(classOf[Reporter], gitClient, twitterClient), "Reporter")

  self ! Start

  override def receive: Receive = {
    case Start => reporter ! FetchReactiveBuzz
    case Finished => context.system.terminate()
  }
}
