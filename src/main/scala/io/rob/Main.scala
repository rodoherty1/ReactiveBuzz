package io.rob

import akka.actor.{ActorLogging, Props, Actor, ActorSystem}

object Main {
  case object FetchReactiveBuzz
}

class Main extends Actor with ActorLogging {

//  import system.dispatcher
  import Main._

  implicit val system = ActorSystem("MyActorSystem")

  val receptionist = context.actorOf(Props[Receptionist], "Receptionist")

  self ! Main.FetchReactiveBuzz

  override def receive: Receive = {
    case Main.FetchReactiveBuzz =>
      log.info("Checking out the buzz about #Reactive applications")
      receptionist ! Main.FetchReactiveBuzz

    case Receptionist.DummyResult =>
      log.info("DummyResult received.")
      context.system.shutdown()
  }

}

