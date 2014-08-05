/**
 * Created by Norman on 20.03.14.
 */
package controllers

import play.api.mvc.{Action, Controller}
import java.io.File
import play.api.libs.json.{Reads, Json}
import scala.io.{Codec, Source}
import extractors.Quad
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.Logger
import java.nio.charset.MalformedInputException


object StatsController extends Controller {

  ExtractorController.savePath

  import play.api.libs.functional.syntax._
  import play.api.libs.json._


  implicit val codec = Codec.UTF8

  case class PageStats(pageName: String,
    revenueYears: Int, operatingIncomeYears: Int, netIncomeYears: Int,
    totalAssetsYears: Int, totalEquityIncrease: Int, employeesYears: Int)

  implicit val quadReads: Reads[Quad] = (
    (JsPath \ "subject").read[String] and
    (JsPath \ "predicate").read[String] and
    (JsPath \ "object").read[String] and
    (JsPath \ "context").read[Map[String,String]]
    )(Quad.apply _)

  implicit val pageStatsWrites = new Writes[PageStats] {
    def writes(pageStats: PageStats) = Json.obj(
      "name" -> pageStats.pageName,
      "revenueYears" -> pageStats.revenueYears,
      "operatingIncomeYears" -> pageStats.operatingIncomeYears,
      "netIncomeYears" -> pageStats.netIncomeYears,
      "totalAssetsYears" -> pageStats.totalAssetsYears,
      "totalEquityIncrease" -> pageStats.totalEquityIncrease,
      "employeeYears" ->  pageStats.employeesYears)
  }


  def getSampleStats = Action.async {
    val extractedStats = scala.concurrent.Future {
      val folder = new File(ExtractorController.savePath)

      val files = folder.listFiles.toList
      val stats = files.map{ case file =>
        try{
        val str = Source.fromFile(file).mkString

        Json.parse(str).validate[List[Quad]] match {
        case s: JsSuccess[List[Quad]] => Some(quadToStats(file.getName, s.get))
        case _ => Logger.error("Skipped stats file: " + file.getName );None
        }
        }catch{
          case mie: MalformedInputException =>
            Logger.error("Mie ata file: " + file.getName ,mie); None
        }
      }.filter(s => s.isDefined).map(s => s.get)


      val sampleSize: Double = stats.size.toDouble
      val assets3Abs = stats.filter(_.totalAssetsYears >= 3).size
      val assets5Abs = stats.filter(_.totalAssetsYears >= 5).size
      val revenue3Abs = stats.filter(_.revenueYears >= 3).size
      val revenue5Abs = stats.filter(_.revenueYears >= 5).size
      val operatingIncome3Abs = stats.filter(_.operatingIncomeYears >= 3).size
      val operatingIncome5Abs = stats.filter(_.operatingIncomeYears >= 5).size
      val netIncome3Abs = stats.filter(_.netIncomeYears >= 3).size
      val netIncome5Abs = stats.filter(_.netIncomeYears >= 5).size
      val equity3Abs = stats.filter(_.totalEquityIncrease >= 3).size
      val equity5Abs = stats.filter(_.totalEquityIncrease >= 5).size
      val employees3Abs = stats.filter(_.totalEquityIncrease >= 3).size
      val employees5Abs = stats.filter(_.totalEquityIncrease >= 5).size

      Json.obj(
       "aggregated" -> Json.obj(
         "sampleSize" -> sampleSize,
         "assets3Abs" -> assets3Abs,
         "assets5Abs" -> assets5Abs,
         "assets3Rel" -> (assets3Abs / sampleSize),
         "assets5Rel" -> (assets5Abs / sampleSize),

         "revenue3Abs" -> revenue3Abs,
         "revenue5Abs" -> revenue5Abs,
         "revenue3Rel" -> (revenue3Abs / sampleSize),
         "revenue5Rel" -> (revenue5Abs / sampleSize),

         "operatingIncome3Abs" -> operatingIncome3Abs,
         "operatingIncome5Abs" -> operatingIncome5Abs,
         "operatingIncome3Rel" -> (operatingIncome3Abs / sampleSize),
         "operatingIncome5Rel" -> (operatingIncome5Abs / sampleSize),

         "netIncome3Abs" -> netIncome3Abs,
         "netIncome5Abs" -> netIncome5Abs,
         "netIncome3Rel" -> (netIncome3Abs / sampleSize),
         "netIncome5Rel" -> (netIncome5Abs / sampleSize),

         "equity3Abs" -> equity3Abs,
         "equity5Abs" -> equity5Abs,
         "equity3Rel" -> (equity3Abs / sampleSize),
         "equity5Rel" -> (equity5Abs / sampleSize),

         "employees3Abs" -> employees3Abs,
         "employees5Abs" -> employees5Abs,
         "employees3Rel" -> (employees3Abs / sampleSize),
         "employees5Rel" -> (employees5Abs / sampleSize)),


      "perPage" -> Json.toJson(stats)
      )


    }
    extractedStats.map(stats => Ok(Json.toJson(stats)))
  }

  private def quadToStats(pageName: String, quads: List[Quad]) = {
    val revenueYears = quads.filter(_.predicate == "http://dbpedia.org/ontology/revenue").filter(_.context.get("fromDate").isDefined).groupBy{_.context.get("fromDate").get}.map{_._1}.size
    val operatingIncomeYears = quads.filter(_.predicate == "http://dbpedia.org/ontology/operatingIncome").filter(_.context.get("fromDate").isDefined).groupBy{_.context.get("fromDate").get}.map{_._1}.size
    val netIncomeYears = quads.filter(_.predicate == "http://dbpedia.org/ontology/netIncome").filter(_.context.get("fromDate").isDefined).groupBy{_.context.get("fromDate").get}.map{_._1}.size
    val totalAssetsYears = quads.filter(_.predicate == "http://dbpedia.org/ontology/assets").filter(_.context.get("fromDate").isDefined).groupBy{_.context.get("fromDate").get}.map{_._1}.size
    val totalEquityIncrease = quads.filter(_.predicate == "http://dbpedia.org/ontology/equity").filter(_.context.get("fromDate").isDefined).groupBy{_.context.get("fromDate").get}.map{_._1}.size
    val employeesYears = quads.filter(_.predicate == "http://dbpedia.org/ontology/numberOfEmployees").filter(_.context.get("fromDate").isDefined).groupBy{_.context.get("fromDate").get}.map{_._1}.size
    PageStats(pageName, revenueYears, operatingIncomeYears, netIncomeYears, totalAssetsYears, totalEquityIncrease, employeesYears)
  }
}
