package example

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.concurrent.Topic
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers

class TopicSpec extends AsyncFlatSpec with Matchers with AsyncIOSpec {
  "Consumer" must "consume last published message in topic" in {
    for {
      topic     <- Topic[IO, Int](0) // Consumer will read 0
      consumed1 <- topic
                     .subscribe(1)
                     .head
                     .compile
                     .toList
      _         <- topic.publish1(1)
      _         <- topic.publish1(2)
      _         <- topic.publish1(3)
      consumed2 <- topic
                     .subscribe(1)
                     .head
                     .compile
                     .toList
    } yield {
      consumed1 must be(List(0))
      consumed2 must be(List(3))
    }
  }
}
