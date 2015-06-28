package io.rob

import akka.actor.{Actor, Props, ActorRef}


class StepParent(child: Props, probe: ActorRef) extends Actor {
  context.actorOf(child, "child")

  override def receive: Receive = {
    case msg => probe.tell(msg, sender())
  }
}
