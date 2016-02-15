package io.rob

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{Http, HttpsContext}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging


import java.security.{SecureRandom, KeyStore}
import javax.net.ssl.{KeyManagerFactory, SSLContext}

import akka.http.scaladsl.server.Directives._

import io.rob.CommonDefs.FetchReactiveBuzz

object Main extends App with LazyLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val serverContext: HttpsContext = {
    val password = "abcdef".toCharArray
    val context = SSLContext.getInstance("TLS")
    val ks = KeyStore.getInstance("PKCS12")
    ks.load(getClass.getClassLoader.getResourceAsStream("keys/server.p12"), password)
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    // start up the web server
    HttpsContext(context)
  }

  import system._

  val route =
    path("posttestserver.com/post.php") {
      post {
        complete {
          "ok"
        }
      }
    }

  Http().bindAndHandle(route, interface = "posttestserver.com", httpsContext = Some(serverContext))


  val reporter = system.actorOf(Props[Reporter], "Reporter")

  logger.info("Checking out the buzz about #Reactive applications")

  reporter ! FetchReactiveBuzz
}

