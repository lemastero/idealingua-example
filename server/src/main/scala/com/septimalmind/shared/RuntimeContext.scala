package com.septimalmind.shared

import java.util.concurrent.{Executors, ThreadPoolExecutor}

import cats.effect.{Clock, Timer}
import com.github.pshirshov.izumi.functional.bio.BIORunner.FailureHandler
import com.github.pshirshov.izumi.functional.bio.{BIOExit, BIORunner}
import com.github.pshirshov.izumi.idealingua.runtime.rpc.http4s.Http4sRuntime
import com.github.pshirshov.izumi.logstage.api.IzLogger
import com.septimalmind.server.idl.RequestContext
import zio.{IO, ZIO}

import scala.concurrent.duration.FiniteDuration
import zio.interop.catz._
import com.github.pshirshov.izumi.logstage.sink.ConsoleSink
import com.github.pshirshov.izumi.logstage.api._
import com.septimalmind.Server

trait RuntimeContext {
  type Runtime = Http4sRuntime[IO, RequestContext, RequestContext, String, Unit, Unit]

  val logger: IzLogger = setupLogger

  implicit val bio: BIORunner[IO] = setupBio(logger)

  implicit val zioClock = zio.clock.Clock.Live

  implicit val timer: Timer[IO[Throwable, ?]] = new Timer[IO[Throwable, ?]] {
    val clock: Clock[IO[Throwable, ?]] = Clock.create[IO[Throwable, ?]]

    override def sleep(duration: FiniteDuration) = {
      zio.clock.sleep(zio.duration.Duration.fromScala(duration)).provide(zioClock)
    }
  }

  def http4sRuntime(implicit timer: Timer[IO[Throwable, ?]]): ZIO[Server.Environment, Nothing, Runtime] = ZIO.runtime.map {
    implicit rt => new Runtime(scala.concurrent.ExecutionContext.global)
  }

  def setupBio(logger: IzLogger): BIORunner[IO] = {
    val cpuPool: ThreadPoolExecutor = Executors.newFixedThreadPool(8).asInstanceOf[ThreadPoolExecutor]

    BIORunner.createZIO(cpuPool,FailureHandler.Custom {
      case BIOExit.Error(error: Throwable, _) =>
        val stackTrace = error.getStackTrace
        IO.effect(logger.warn(s"Fiber terminated with unhandled Throwable $error $stackTrace"))
      case BIOExit.Error(error, _) =>
        IO.effect(logger.warn(s"Fiber terminated with unhandled $error"))
      case BIOExit.Termination(_, (_: InterruptedException) :: _, _) =>
        IO.unit // don't log interrupts
      case BIOExit.Termination(exception, _, _) =>
        IO.effect(logger.warn(s"Fiber terminated erroneously with unhandled defect $exception"))
    }, 1024)
  }

  def setupLogger: IzLogger = {
    val textSink = ConsoleSink.text(colored = true)
    val sinks = List(textSink)
    IzLogger.apply(Log.Level.Trace, sinks)
  }
}
