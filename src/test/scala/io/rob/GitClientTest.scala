package io.rob

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import io.rob.GitClient.{ReactiveProjects, GitProject, GitResult}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike, FunSuite}

import scala.concurrent.{Promise, Future}

class GitClientTest extends TestKit(ActorSystem("GitClientSpec")) with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  def uuid = java.util.UUID.randomUUID

  override def afterAll(): Unit = {
    system.shutdown()
  }

  val reactiveProjectName = "Some Reactive Project"

  def createGitClient(): Props = {
    Props(new GitClient() {
      override def fetchReactiveProjectsFromGit(): Future[GitResult] = {
        val p = Promise[GitResult]()
        p.success(GitResult(1, incomplete_results = false, List(GitProject(reactiveProjectName))))
        p.future
      }
    })
  }

  "My GitClient Actor" must {
    "send #Reactive projects to the Controller" in {
      val controller = system.actorOf(Props(new StepParent(createGitClient(), "gitClient", testActor)))

      val gitClient = TestActorRef[GitClient]

      val id = uuid
      gitClient ! GetReactiveProjects(id)

      expectMsg(ReactiveProjects(id, List(reactiveProjectName)))
    }
  }
}
