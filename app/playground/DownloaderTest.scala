package playground

import extractors.RelevantRevisionDownloader

/**
 * Created by Norman on 01.04.14.
 */
object DownloaderTest extends App {

  val start = System.currentTimeMillis()
  val result  = RelevantRevisionDownloader.download("Alternative_Energy_Development_Board")
  val stop = System.currentTimeMillis()

  println(stop - start)
}
