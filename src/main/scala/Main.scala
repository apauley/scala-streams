
import scala.util.Properties.{javaVmVersion, versionMsg}
import zio.*

object Main extends ZIOAppDefault:
  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    ZIO.logInfo(s"Hello, world $javaVmVersion $versionMsg")