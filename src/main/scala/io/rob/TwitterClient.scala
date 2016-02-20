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
    case getTweets@GetTweets(id, _) =>
      tweets = getTweets :: tweets
      context.become(authenticating(sender()))
      self ! Authenticate
  }

  def authenticating(reporter: ActorRef): Receive = {
    case Authenticate =>
      Http().singleRequest(authRequest) onComplete {
        case Success(response) => Unmarshal(response.entity).to[OAuthToken] onComplete {
          case Success(token) =>
            context.become(authenticated(reporter, Authorization(OAuth2BearerToken(token.access_token))))
            tweets.foreach(self ! _)
            tweets = List.empty
          case _ => reporter ! FailedToAuthenticate
        }

        case _  => reporter ! FailedToAuthenticate
      }

    case getTweets@GetTweets(id, _) => tweets = getTweets :: tweets
  }

  def authenticated(reporter: ActorRef, token: Authorization): Receive = {
    case GetTweets(id, queryParam) =>
      val request = buildGetTweetsRequest(token, queryParam)

      Http().singleRequest(request).onComplete {
        case Success(HttpResponse(status, _, entity, _)) => Unmarshal(entity).to[Tweets].onSuccess {
          case Tweets(statuses) => reporter ! TwitterResult(id, queryParam, statuses.headOption)
        }
        case Failure(ex) =>
          context.become(notAuthenticated())
          log.error(ex.getMessage, ex)
      }
  }

  def buildGetTweetsRequest(token: Authorization, queryParam: String): HttpRequest = {
    val params = Map("q" -> queryParam)

    HttpRequest(
      uri = Uri("https://api.twitter.com/1.1/search/tweets.json").withQuery(Uri.Query(params)),
      method = HttpMethods.GET,
      headers = List(token)
    )
  }
}
