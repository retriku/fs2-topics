package example

import cats.data.NonEmptyList
import cats.effect._
import dev.profunktor.fs2rabbit.config.Fs2RabbitConfig
import dev.profunktor.fs2rabbit.config.Fs2RabbitNodeConfig
import dev.profunktor.fs2rabbit.interpreter.RabbitClient

import dev.profunktor.fs2rabbit.resiliency.ResilientStream
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.lang.Thread.UncaughtExceptionHandler
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object IOAckerWrongOrderConsumer extends IOApp {

  private val config: Fs2RabbitConfig = Fs2RabbitConfig(
      virtualHost = "/",
      nodes = NonEmptyList.one(Fs2RabbitNodeConfig(host = "127.0.0.1", port = 5672)),
      username = Some("guest"),
      password = Some("guest"),
      ssl = false,
      connectionTimeout = 3000,
      requeueOnNack = false,
      requeueOnReject = false,
      internalQueueSize = Some(500),
      requestedHeartbeat = 60000,
      automaticRecovery = true,
  )

  val blockerResource: Resource[IO, Blocker] =
    Resource
      .make(IO.delay(Executors.newCachedThreadPool(new ThreadFactory {
        val defaultThreadFactory   = Executors.defaultThreadFactory()
        val idx                    = new AtomicInteger(0)
        def newThread(r: Runnable) = {
          val t = defaultThreadFactory.newThread(r)
          t.setDaemon(true)
          t.setName(s"rabbit-${idx.incrementAndGet()}")
          t.setUncaughtExceptionHandler(new UncaughtExceptionHandler {
            def uncaughtException(t: Thread, e: Throwable): Unit = {
              ExecutionContext.defaultReporter(e)
              e match {
                case NonFatal(_) => ()
                case _           => System.exit(-1)
              }
            }
          })
          t
        }
      })))(ec => IO.delay(ec.shutdown()))
      .map(Blocker.liftExecutorService)

  override def run(args: List[String]): IO[ExitCode] =
    blockerResource.use { blocker =>
      for {
        client <- RabbitClient[IO](config, blocker)
        _      <- ResilientStream.runF(new AckerConsumerWrongOrderDemo[IO](client).program)
      } yield ExitCode.Success
    }
}
