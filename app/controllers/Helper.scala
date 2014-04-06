package controllers

import scala.io.{Codec, Source}
import java.io.File
import play.api.libs.Files
import play.api.Logger


object Helper {

  implicit val codec = Codec.UTF8

  val companyResourcesCacheFilePath = "data/companyResourceUris.txt"
  val randomSampleFile = "data/randomSample.txt"


  def writeCompanyResourcesFile(companiesResourceUris: List[String]) =
    writeFile(companyResourcesCacheFilePath, companiesResourceUris.mkString("\n"))

  def readCompanyResourceUris: List[String] =
    readFileFromPathOrElse(companyResourcesCacheFilePath, {
      source => source.getLines().toList
    }, {
      List.empty[String]
    })

  def writeRandomSampleFile(sample: List[String]) =
    writeFile(randomSampleFile, sample.mkString("\n"))

  def readRandomSample: List[String] =
    readFileFromPathOrElse(randomSampleFile, {
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
    Logger.info("ensure exists:" + path)
    val f = new File(path)
    if (!f.exists()) {
      f.getParentFile.mkdirs
    }
    f
  }

}
