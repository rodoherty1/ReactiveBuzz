package io.rob

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.rob.CommonDefs.FetchReactiveBuzz

object Main extends App with LazyLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val reporter = system.actorOf(Props[Reporter], "Reporter")

  logger.info("Checking out the buzz about #Reactive applications")

  reporter ! FetchReactiveBuzz
}

