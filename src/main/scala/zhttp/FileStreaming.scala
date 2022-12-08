package zhttp

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.stream.ZStream
import zutil.StreamHelpers

import java.io.File
import java.nio.file.Paths

object FileStreaming extends ZIOAppDefault {

  val testVideo = "src/main/resources/TestVideoFile.m4v"

  // Create HTTP route
  val app = Http.collectHttp[Request] {
    case Method.GET -> !! / "health" => Http.ok

    // Read the file as ZStream
    // Uses the blocking version of ZStream.fromFile
    case Method.GET -> !! / "blocking" => Http.fromStream(ZStream.fromPath(Paths.get(testVideo)))
    case Method.GET -> !! / "filezio" => Http.fromFileZIO(ZIO.succeed(new File(testVideo)))

    case Method.GET -> !! / "receive" => Http.fromStream(StreamHelpers.tickStream(1000L, 10))
    case Method.GET -> !! / "q" => Http.fromStream(StreamHelpers.sampleQStream("Hello bleep"))


    // Uses netty's capability to write file content to the Channel
    // Content-type response headers are automatically identified and added
    // Adds content-length header and does not use Chunked transfer encoding
    case Method.GET -> !! / "video" => Http.fromFile(new File(testVideo))
    case Method.GET -> !! / "text"  => Http.fromFile(new File("src/main/resources/TestFile.txt"))
  }

  // Run it like any simple app
  val run =
    Server.serve(app).provide(Server.default)
}