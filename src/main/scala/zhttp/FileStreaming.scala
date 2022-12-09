package zhttp

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.stream.ZStream
import util.ZHelpers

import java.io.File
import java.nio.file.Paths

object FileStreaming extends ZIOAppDefault {

  val testVideo = "src/main/resources/TestVideoFile.m4v"

  // Create HTTP route
  val app: Http[Any, Throwable, Request, Response] = Http.collectHttp[Request] {
    case Method.GET -> !! / "health" => Http.ok

    // Read the file as ZStream
    // Uses the blocking version of ZStream.fromFile
    case Method.GET -> !! / "blocking" =>
      Http.fromStream(ZStream.fromPath(Paths.get(testVideo)))
    case Method.GET -> !! / "filezio" =>
      Http.fromFileZIO(ZIO.succeed(new File(testVideo)))

    case Method.GET -> !! / "receive" =>
      Http.fromStream(ZHelpers.tickStream(1000000L, 1.millis))

    case Method.GET -> !! / "q" =>
      Http.fromStream {
        ZStream.unwrap {
          for {
            q <- Queue.bounded[String](4096)
            _ <- AppLogic.doQueueStuff(q).fork
          } yield ZStream.fromQueueWithShutdown(q)
        }
      }

    case Method.GET -> !! / "random" =>
      Http.fromStream(ZHelpers.randomInts.map(i => s"$i "))

    // Uses netty's capability to write file content to the Channel
    // Content-type response headers are automatically identified and added
    // Adds content-length header and does not use Chunked transfer encoding
    case Method.GET -> !! / "video" => Http.fromFile(new File(testVideo))
    case Method.GET -> !! / "text" =>
      Http.fromFile(new File("src/main/resources/TestFile.txt"))
  }

  // Run it like any simple app
  val run =
    Server.serve(app).provide(Server.default)
}

object AppLogic {
  def doQueueStuff(q: Queue[String]): Task[Unit] = {
    for {
      now <- Clock.currentDateTime
      _ <- q.offer(s"$now\n")
      _ <- ZIO.sleep(1.milli)
    } yield ()
  }.forever
}
