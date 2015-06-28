package io.rob

import akka.actor.{Props, Actor}


object Receptionist {
  case object FetchReactiveBuzz
  case object DummyResult
}

class Receptionist extends Actor {

  val controller = context.actorOf(Props[Controller], "Controller")

  override def receive: Receive = {
    case Main.FetchReactiveBuzz => controller ! Receptionist.FetchReactiveBuzz
  }
}
