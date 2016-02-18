package io.rob

import java.nio.charset.StandardCharsets
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, RawHeader, BasicHttpCredentials, Authorization}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{ DefaultFormats, jackson }

import java.util.Base64

import io.rob.CommonDefs.FetchReactiveBuzz

import scala.concurrent.Future
import scala.util.{Success, Failure}

object Main extends App with LazyLogging {

  case class Token(access_token: String)
  case class Tweets(statuses: Seq[Tweet])
  case class Tweet(text: String)

  import Json4sSupport._

  implicit val serialization = jackson.Serialization // or native.Serialization
  implicit val formats = DefaultFormats

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val key = ""
  val secret = ""

  val authorization = Authorization(BasicHttpCredentials(key, secret))

  Http().singleRequest(HttpRequest(
    uri = Uri("https://api.twitter.com/oauth2/token"),
    method = HttpMethods.POST,
    headers = List(authorization),
    entity = FormData("grant_type" -> "client_credentials").toEntity,
    protocol = HttpProtocols.`HTTP/1.1`)).onComplete {

    case Success(r) ⇒
      logger.info(r.toString)
      val token = Unmarshal(r.entity).to[Token]
      token.onSuccess {
        case Token(accessToken) => {
          val params = Map("q" -> "rob_odoherty")


          Http().singleRequest(HttpRequest(
            uri = Uri("https://api.twitter.com/1.1/search/tweets.json").withQuery(Uri.Query(params)),
            method = HttpMethods.GET,
            headers = List(Authorization(OAuth2BearerToken(accessToken)))
          )).onComplete {
            case Success(HttpResponse(status, _, entity, _)) => Unmarshal(entity).to[Tweets].onSuccess {
              case Tweets(tweets) => tweets.foreach(tweet => logger.info(tweet.text))
            }
            case Failure(ex) => logger.error(ex.getMessage, ex)
          }
        }

        //      logger.info(token.toString)
        //      val reporter = system.actorOf(Props[Reporter], "Reporter")
        //      logger.info("Checking out the buzz about #Reactive applications")
        //       reporter ! FetchReactiveBuzz
      }
    case Failure(e) ⇒ logger.error(e.getMessage, e)
  }

}

