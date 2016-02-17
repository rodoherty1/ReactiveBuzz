package io.rob

import java.nio.charset.StandardCharsets
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{RawHeader, BasicHttpCredentials, Authorization}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging

import java.util.Base64

import io.rob.CommonDefs.FetchReactiveBuzz

import scala.util.{Success, Failure}

object Main extends App with LazyLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val key = ""
  val secret = ""

  val encodedKeySecret = Base64.getEncoder.encodeToString(s"$key:$secret".getBytes(StandardCharsets.UTF_8))

  val authorization = Authorization(BasicHttpCredentials(key, secret))
  val contentType: HttpHeader = RawHeader("Content-Type", "application/x-www-form-urlencoded;")

  Http().singleRequest(HttpRequest(
    uri = Uri("https://api.twitter.com/oauth2/token"),
    method = HttpMethods.POST,
    headers = List(authorization, contentType),
    entity = FormData("grant_type" -> "client_credentials").toEntity,
    protocol = HttpProtocols.`HTTP/1.1`)).onComplete {

    case Success(r) ⇒
      logger.info(r.toString)
      val reporter = system.actorOf(Props[Reporter], "Reporter")
      logger.info("Checking out the buzz about #Reactive applications")
       reporter ! FetchReactiveBuzz
    case Failure(e) ⇒ logger.error(e.getMessage, e)
  }

  protected def base64Encode(key: String, secret: String): String = {
    Base64.getEncoder.encodeToString(s"$key:$secret".getBytes(StandardCharsets.UTF_8))
  }

}

