package zutil

import zio._
import zio.stream.{Stream, UStream, ZStream}

import java.time.Duration

object StreamHelpers {
  def tickStream(size: Long, millis: Int): UStream[Byte] = ZStream
    .tick(Duration.ofMillis(millis))
    .zipWith(ZStream[Char]('a', 'b', 'c', 'd').repeat(Schedule.forever))((_, c) => c)
    .take(size)
    .map(_.toByte)
    .tap(x => ZIO.logInfo("XXX " + x))

  def sampleQStream[A](initialValue: A): UStream[A] = ZStream.unwrap {
    Queue.unbounded[A].tap{ q =>
      ZIO.logInfo(s"Offering initial value: $initialValue") *> q.offer(initialValue)
    }
    .map(qStream)
  }

  def qStream[A](q: Queue[A]): UStream[A] =
    ZStream.fromQueueWithShutdown(q)
}
