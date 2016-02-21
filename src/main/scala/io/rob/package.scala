package io

/**
  * Created by rob on 19/02/16.
  */

package rob {

  import java.util.UUID

  case class GetReactiveProjects(id: UUID)
  case object FetchReactiveBuzz
  case object DummyResult

  case object Authenticate
  case class GetTweets(uuid: UUID, queryParam: String)
  case class OAuthToken(access_token: String)
//  case object FailedToAuthenticate

  case class Tweets(statuses: Seq[Tweet])
  case class Tweet(text: String)
  case class TwitterResult(id: UUID, queryParam: String, tweet: Option[Tweet])

  case class PrintReport(id: UUID)

  case class Error(msg: String)
  case object Finished
}

