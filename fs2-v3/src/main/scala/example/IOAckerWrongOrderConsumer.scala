package example

import cats.data.NonEmptyList
import cats.effect._
import dev.profunktor.fs2rabbit.config.Fs2RabbitConfig
import dev.profunktor.fs2rabbit.config.Fs2RabbitNodeConfig
import dev.profunktor.fs2rabbit.interpreter.RabbitClient

import scala.concurrent.duration.DurationInt
import dev.profunktor.fs2rabbit.resiliency.ResilientStream

object IOAckerWrongOrderConsumer extends IOApp.Simple {

  private val config: Fs2RabbitConfig = Fs2RabbitConfig(
    virtualHost = "/",
    nodes = NonEmptyList.one(Fs2RabbitNodeConfig(host = "127.0.0.1", port = 5672)),
    username = Some("guest"),
    password = Some("guest"),
    ssl = false,
    connectionTimeout = 3.seconds,
    requeueOnNack = false,
    requeueOnReject = false,
    internalQueueSize = Some(500),
    requestedHeartbeat = 60.seconds,
    automaticRecovery = true
  )

  override def run: IO[Unit] =
    RabbitClient.resource[IO](config).use { client =>
      ResilientStream
        .runF(new AckerConsumerWrongOrderDemo[IO](client).program)
    }
}
