package tapir

import sttp.capabilities.zio.ZioStreams
import sttp.model.HeaderNames
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.*
import sttp.tapir.{CodecFormat, PublicEndpoint}
import util.ZHelpers
import zio.http.{HttpApp, Server, ServerConfig}
import zio.stream.*
import zio._

import java.nio.charset.StandardCharsets
import java.time.Duration

object StreamingZioHttpServer extends ZIOAppDefault {
  // corresponds to: GET /receive?name=...
  // We need to provide both the schema of the value (for documentation), as well as the format (media type) of the
  // body. Here, the schema is a `string` (set by `streamTextBody`) and the media type is `text/plain`.
  val streamingEndpoint: PublicEndpoint[Unit, Unit, (Long, Stream[Throwable, Byte]), ZioStreams] =
    endpoint.get
      .in("receive")
      .out(header[Long](HeaderNames.ContentLength))
      .out(streamTextBody(ZioStreams)(CodecFormat.TextPlain(), Some(StandardCharsets.UTF_8)))

  // converting an endpoint to a route (providing server-side logic)
  val streamingServerEndpoint: ZServerEndpoint[Any, ZioStreams] = streamingEndpoint.zServerLogic { _ =>
    val size = 100L
    val stream = ZHelpers.tickStream(size, 100.millis)
    ZIO.succeed((size, stream))
  }

  val qStreamingEndpoint: PublicEndpoint[Unit, Unit, Stream[Throwable, Byte], ZioStreams] =
    endpoint.get
      .in("q")
      .out(
        streamTextBody(ZioStreams)(
          format = CodecFormat.TextPlain(),
          charset = Some(StandardCharsets.UTF_8),
        ),
      )

  val qStreamingServerEndpoint: ZServerEndpoint[Any, ZioStreams] = qStreamingEndpoint.zServerLogic { _ =>
    val stream: UStream[Byte] = ZHelpers.sampleQStream.map(s => Chunk.fromArray(s.getBytes)).flattenChunks
    ZIO.succeed(stream)
  }

  val all = List(streamingServerEndpoint, qStreamingServerEndpoint)

  val routes: HttpApp[Any, Throwable] = ZioHttpInterpreter().toHttp(all)

  // Test using: curl http://localhost:8080/receive
  override def run: URIO[Any, ExitCode] =
    Server
      .serve(routes)
      .provide(
        ServerConfig.live(ServerConfig.default.port(8080)),
        Server.live,
      )
      .exitCode
}
