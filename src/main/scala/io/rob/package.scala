package io

/**
  * Created by rob on 19/02/16.
  */

package rob {

  case object Authenticate
  case class GetTweets(hashtag: String)
  case class OAuthToken(access_token: String)
  case object FailedToAuthenticate

  case class Tweets(statuses: Seq[Tweet])
  case class Tweet(text: String)
}

