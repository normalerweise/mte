package controllers

import scala.io.{Codec, Source}
import java.io.File
import play.api.libs.Files


object FileUtil {

  implicit val codec = Codec.UTF8

  def cacheFilePathForQueryId(queryId: String) = s"data/${queryId}Uris.txt"

  def writeOneElementPerLine(filePath: String, list: List[String]) =
    writeFile(filePath, list.mkString("\n"))

  def readFileOneElementPerLine(filePath: String): List[String] =
    readFileFromPathOrElse(filePath, {
      source => source.getLines().toList
    }, {
      List.empty[String]
    })

  private def readFileFromPathOrElse[E](path: String, exists: Source => E, default: => E) = {
    val f = new File(path)
    if (f.exists && !f.isDirectory) exists(Source.fromFile(f))
    else default
  }

  private def writeFile(path: String, content: String) =
    Files.writeFile(ensureExists(path), content)


  def ensureExists(path: String) = {
    val f = new File(path)
    if (!f.exists()) {
      f.getParentFile.mkdirs
    }
    f
  }

}
