package io.rob

import akka.actor.{Actor, Props, ActorRef}


class StepParent(child: Props, childName: String, probe: ActorRef) extends Actor {
  context.actorOf(child, childName)

  override def receive: Receive = {
    case msg => probe.tell(msg, sender())
  }
}
