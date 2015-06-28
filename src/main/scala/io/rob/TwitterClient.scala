package io.rob

import akka.actor.Actor
import io.rob.TwitterClient.GetTweets


object TwitterClient {
  case class GetTweets(hashtag: String)
}

class TwitterClient extends Actor {

  override def receive: Receive = {
    case GetTweets(hashtag) => ???
  }
}
