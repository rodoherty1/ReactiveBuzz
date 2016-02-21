package io.rob

import akka.actor.{ActorSystem, Props}

object Main extends App {

  implicit val system = ActorSystem()

  system.actorOf(Props[Controller], "Controller")
}

