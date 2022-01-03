package example

import cats.data.Kleisli
import cats.effect._
import cats.effect.syntax.spawn._
import cats.implicits._
import dev.profunktor.fs2rabbit.config.declaration
import dev.profunktor.fs2rabbit.config.declaration.DeclarationExchangeConfig
import dev.profunktor.fs2rabbit.config.declaration.DeclarationQueueConfig
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import dev.profunktor.fs2rabbit.effects.MessageEncoder
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.AmqpFieldValue.LongVal
import dev.profunktor.fs2rabbit.model.AmqpFieldValue.StringVal
import dev.profunktor.fs2rabbit.model.ExchangeType.Topic
import dev.profunktor.fs2rabbit.model._
import fs2._

import AckerConsumerWrongOrderDemo._

class AckerConsumerWrongOrderDemo[F[_]: Async](fs2Rabbit: RabbitClient[F]) {
  private val queueName    = QueueName("testQ")
  private val exchangeName = ExchangeName("testEX")
  private val routingKey   = RoutingKey("testRK")

  implicit val intMessageDecoder: EnvelopeDecoder[F, Int] =
    Kleisli[F, AmqpEnvelope[Array[Byte]], Int](s => new String(s.payload).toInt.pure[F])

  implicit val intMessageEncoder: MessageEncoder[F, AmqpMessage[Int]] =
    Kleisli[F, AmqpMessage[Int], AmqpMessage[Array[Byte]]](s => s.copy(payload = s.payload.toString.getBytes).pure[F])

  private val mkChannel = fs2Rabbit.createConnection.flatMap(fs2Rabbit.createChannel)

  val program: F[Unit] = mkChannel.use { implicit channel =>
    for {
      _         <- fs2Rabbit.declareQueue(DeclarationQueueConfig.default(queueName).copy(autoDelete = declaration.AutoDelete))
      _         <- fs2Rabbit.declareExchange(DeclarationExchangeConfig.default(exchangeName, Topic))
      _         <- fs2Rabbit.bindQueue(queueName, exchangeName, routingKey)
      consumer  <- fs2Rabbit.createAutoAckConsumer[Int](queueName)
      publisher <- fs2Rabbit.createPublisher[AmqpMessage[Int]](
                       exchangeName,
                       routingKey,
                   )
      _         <- new WrongOrderFlow[F](consumer, publisher).flow.take(200).compile.drain
    } yield ()
  }

}

object AckerConsumerWrongOrderDemo {
  def putStrLnThread[F[_]: Sync, A](a: A): F[Unit] = Sync[F].delay(println(s"${Thread.currentThread().getName()} - $a"))
}

class WrongOrderFlow[F[_]: Async](
        consumer: Stream[F, AmqpEnvelope[Int]],
        publisher: AmqpMessage[Int] => F[Unit],
) {
  def simpleMessage(i: Int) =
    AmqpMessage(i, AmqpProperties(headers = Map("demoId" -> LongVal(123), "app" -> StringVal("fs2RabbitDemo"))))

  val flow: Stream[F, Unit] =
    for {
      counter <- Stream.eval(Ref.of(0))
      prev    <- Stream.eval(Ref.of(0))
      _       <- Stream.resource(
                     Stream
                       .eval(counter.getAndUpdate(_ + 1).map(simpleMessage))
                       .evalTap(publisher)
                       .evalTap(i => putStrLnThread(s"Published ${i.payload}"))
                       .repeatN(200)
                       .compile
                       .drain
                       .background
                 )
      _       <-
        consumer
          .evalTap(env => putStrLnThread(s"Consumed ${env.payload}"))
          .evalTap(i =>
            prev.get.flatMap { p =>
              if ((i.payload - p) != 1 && (i.payload > 0 || p > 0))
                putStrLnThread(s"Expected ${p + 1} got ${i.payload}")
              else ().pure[F]
            })
          .evalTap(m => prev.set(m.payload))
    } yield ()
}
