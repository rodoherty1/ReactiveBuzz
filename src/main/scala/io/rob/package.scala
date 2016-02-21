package io

package rob {

  import java.util.UUID

  case class GetReactiveProjects(id: UUID)
  case object FetchReactiveBuzz

  case object Authenticate
  case class GetTweets(uuid: UUID, queryParam: String)
  case class OAuthToken(access_token: String)

  case class Tweets(statuses: Seq[Tweet])
  case class Tweet(text: String)
  case class TwitterResult(id: UUID, queryParam: String, tweets: Seq[Tweet])

  case class PrintReport(id: UUID)

  case class Error(msg: String)
  case object Finished
}

