package io.rob

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class ControllerTest extends TestKit(ActorSystem("ControllerSpec")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  override def afterAll(): Unit = {
    system.shutdown()
  }

  def fakeController(testActor: ActorRef) = {
    Props(new Controller() {
      //override def createGitClient() = testActor
    })
  }

  "A Controller" must {
    "Request Reactive projects from the GitClient" in {
      val gitClient = system.actorOf(Props[GitClient])
      val controller = system.actorOf(fakeController(testActor))
      controller ! FetchReactiveBuzz
      expectMsg(GetReactiveProjects)
    }
  }


}
