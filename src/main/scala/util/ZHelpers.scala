package util

import zio._
import zio.stream.{Stream, UStream, ZStream}

import zio.Duration

object ZHelpers {
  def tickStream(size: Long, interval: Duration): UStream[Byte] = ZStream
    .tick(interval)
    .zipWith(ZStream[Char]('a', 'b', 'c', 'd').repeat(Schedule.forever))((_, c) => c)
    .take(size)
    .map(_.toByte)
    .tap(x => ZIO.logInfo("XXX " + x))

  val randomInts: ZStream[Any, Throwable, Int] = ZStream.repeatZIO {
    Random.nextIntBounded(100)
  }

  val sampleQStream: UStream[String] = ZStream.unwrap {
    for {
      q <- Queue.bounded[String](1024)
      _ <- doQueueStuff(q).fork
    } yield ZStream.fromQueueWithShutdown(q)
  }

  def doQueueStuff(q: Queue[String]): Task[Unit] = {
    for {
      now <- Clock.currentDateTime
      _ <- q.offer(s"$now\n")
      _ <- ZIO.sleep(1.milli)
    } yield ()
  }.forever

}
