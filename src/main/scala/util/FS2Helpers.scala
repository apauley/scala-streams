package util

import cats.effect.IO
import cats.implicits.*

import fs2.{Chunk, Stream}

import scala.concurrent.duration.FiniteDuration

object FS2Helpers {

  def tickStream(size: Long, rate: FiniteDuration): Stream[IO, Byte] = {
    Stream
      .emit(List[Char]('a', 'b', 'c', 'd'))
      .repeat
      .flatMap(list => Stream.chunk(Chunk.seq(list)))
      .metered[IO](rate)
      .take(size)
      .covary[IO]
      .map(_.toByte)
  }
}
