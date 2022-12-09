package util

import zio._
import zio.stream.{Stream, UStream, ZStream}

import zio.Duration

object ZHelpers {
  def tickStream(size: Long, interval: Duration): UStream[Byte] = ZStream
    .tick(interval)
    .zipWith(ZStream[Char]('a', 'b', 'c', 'd').repeat(Schedule.forever))(
      (_, c) => c
    )
    .take(size)
    .map(_.toByte)
    .tap(x => ZIO.logInfo("XXX " + x))

  val randomInts: ZStream[Any, Throwable, Int] = ZStream.repeatZIO {
    Random.nextIntBounded(100).tap { i =>
      Console.print(s"$i ")
    }
  }

  def sampleQStream[A](initialValue: A): UStream[A] = ZStream.unwrap {
    Queue
      .bounded[A](8)
      .tap { q =>
        ZIO.logInfo(s"Offering initial value: $initialValue") *> 
          q.offer(initialValue)
      }
      .map(qStream)
  }

  def qStream[A](q: Queue[A]): UStream[A] =
    ZStream.fromQueueWithShutdown(q)
}
