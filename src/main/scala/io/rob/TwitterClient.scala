package io.rob

import akka.actor.{ActorRef, ActorSystem, ActorLogging, Actor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, BasicHttpCredentials, Authorization}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, jackson}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object TwitterClient {

  case object Authenticated

  val config = ConfigFactory.load()
  val key = config.getString("consumer-key")

  val secret = config.getString("consumer-secret")

  val authorization = Authorization(BasicHttpCredentials(key, secret))


  val authRequest = HttpRequest(
    uri = Uri("https://api.twitter.com/oauth2/token"),
    method = HttpMethods.POST,
    headers = List(authorization),
    entity = FormData("grant_type" -> "client_credentials").toEntity,
    protocol = HttpProtocols.`HTTP/1.1`)
}

class TwitterClient extends Actor with ActorLogging {

  import TwitterClient._

  import Json4sSupport._

  implicit val serialization = jackson.Serialization
  implicit val formats = DefaultFormats

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = context.dispatcher

  var tweets: List[GetTweets] = List[GetTweets]()

  override def receive: Receive = notAuthenticated()

  def notAuthenticated(): Receive = {
    case getTweets@GetTweets(hashtag) =>
      tweets = getTweets :: tweets
      self ! Authenticate

    case Authenticate =>
      val reporter = sender()
      Http().singleRequest(authRequest) onComplete {
        case Success(response) => Unmarshal(response.entity).to[OAuthToken] onComplete {
          case Success(token) =>
            context.become(authenticated(reporter, Authorization(OAuth2BearerToken(token.access_token))))
            self ! Authenticated
          case _ => reporter ! FailedToAuthenticate
        }

        case _  => reporter ! FailedToAuthenticate
      }
  }


  def authenticated(reporter: ActorRef, token: Authorization): Receive = {

    case Authenticated => tweets match {
      case Nil => ()
      case h :: t =>
        tweets = t
        self ! h
    }

    case GetTweets(hashtag) =>
      val params = Map("q" -> hashtag)
      Http().singleRequest(HttpRequest(
        uri = Uri("https://api.twitter.com/1.1/search/tweets.json").withQuery(Uri.Query(params)),
        method = HttpMethods.GET,
        headers = List(token))
      ).onComplete {
        case Success(HttpResponse(status, _, entity, _)) => Unmarshal(entity).to[Tweets].onSuccess {
          case Tweets(statuses) => statuses.foreach(tweet => log.info(tweet.text))
        }
        case Failure(ex) => log.error(ex.getMessage, ex)
      }
  }
}
