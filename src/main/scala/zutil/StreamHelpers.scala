package zutil

import zio._
import zio.stream.ZStream

import java.time.Duration

object StreamHelpers {
  def tickStream(size: Long, millis: Int) = ZStream
    .tick(Duration.ofMillis(millis))
    .zipWith(ZStream[Char]('a', 'b', 'c', 'd').repeat(Schedule.forever))((_, c) => c)
    .take(size)
    .map(_.toByte)
    .tap(x => ZIO.logInfo("XXX " + x))
}
