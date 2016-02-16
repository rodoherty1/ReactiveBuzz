package io.rob

import java.nio.charset.StandardCharsets
import java.security.cert.CertificateFactory

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{HttpMethods, HttpMethod, Uri, HttpRequest}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{DefaultSSLContextCreation, Http, HttpsContext}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import java.security.cert.{ CertificateFactory, Certificate }
import java.security.{SecureRandom, KeyStore}
import javax.net.ssl.{ SSLParameters, SSLContext, TrustManagerFactory, KeyManagerFactory }
import java.io.InputStream
import java.util.Base64

import io.rob.CommonDefs.FetchReactiveBuzz

import scala.util.{Success, Failure}

object Main extends App with LazyLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()


  Http().singleRequest(HttpRequest(uri = Uri("https://api.twitter.com/oauth2/token"), method = HttpMethods.POST)).onComplete {
    case Success(r) ⇒ {
      logger.info(r.toString)
      val reporter = system.actorOf(Props[Reporter], "Reporter")
      logger.info("Checking out the buzz about #Reactive applications")
      reporter ! FetchReactiveBuzz
    }
    case Failure(e) ⇒ logger.error(e.getMessage, e)
  }

  protected def base64Encode(key: String, secret: String): String = {
    Base64.getEncoder.encodeToString("user:pass".getBytes(StandardCharsets.UTF_8))
  }

}

