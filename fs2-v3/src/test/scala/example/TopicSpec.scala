package example

import cats.effect.IO
import cats.effect.Outcome
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.concurrent.Topic
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers

import java.util.concurrent.TimeoutException
import scala.concurrent.duration._

class TopicSpec extends AsyncFlatSpec with Matchers with AsyncIOSpec {
  "Message" must "be lost if no cosumers" in {
    for {
      topic <- Topic[IO, Int]
      _     <- topic.publish1(1)
      e     <- topic.subscribe(1).head.timeout(1.second).compile.toList.attempt
    } yield e.fold(_ mustBe a[TimeoutException], _ => fail())
  }

  it must "be consumed when subscription is active" in {
    for {
      topic    <- Topic[IO, Int]
      _        <- topic.publish1(1) // This is lost
      f        <- topic.subscribe(1).head.compile.toList.start
      _        <- topic.publish1(2) // This is lost
      _        <- IO.sleep(1.millis)
      _        <- topic.publish1(3)
      out      <- f.join
      consumed <- out match {
                    case Outcome.Succeeded(r) => r
                    case _                    => IO.raiseError(new AssertionError())
                  }
    } yield consumed must be(List(3))
  }
}
