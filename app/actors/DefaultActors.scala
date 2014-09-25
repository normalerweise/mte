package actors

import scala.concurrent.duration._
import scala.concurrent.Await
import akka.actor.{ActorSystem, ActorRef, Props}
import akka.routing.BalancingPool
import akka.util.Timeout
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actors.supervision.{CreateActor, MteSupervisor}



object DefaultActors {
  implicit val timeout = Timeout(20 seconds)

  private val numberOfExtractorWorkers = {
    val noOfProcessors = Runtime.getRuntime.availableProcessors
    // use a maximum of 80% of available processors for the extraction
    (noOfProcessors * 0.8).toInt
  }

  val extractionSystem = ActorSystem("extraction-system")

  lazy val supervisor = extractionSystem.actorOf(Props[MteSupervisor],"supervisor")

  lazy val dbPedia = Await.result( supervisor ? CreateActor(Props[DBpediaSparqlEndpointActor], "dbpediaSparql")
    map{ case a: ActorRef => a}, 1 second)


  lazy val pageDownloader = Await.result(
    supervisor ? CreateActor(BalancingPool(3).props(Props[ArticleDownloaderActor]),"wikipediaDownloader")
      map { case a: ActorRef => a}, 1 second)


  lazy val wikiMarkupToTextConverter = Await.result(
    supervisor ? CreateActor(BalancingPool(numberOfExtractorWorkers).props(Props[WikiMarkupToTextConverterActor]),"wikiMarkupToTextConverter")
      map { case a: ActorRef => a}, 1 second)


  lazy val infoboxExtractor = Await.result(
    supervisor ? CreateActor(BalancingPool(numberOfExtractorWorkers).props(Props[TemporalInfoboxExtractorActor]),"infoboxExtractor")
      map{ case a: ActorRef => a}, 20 seconds)

  lazy val sampleFinder = Await.result(
    supervisor ? CreateActor(BalancingPool(numberOfExtractorWorkers).props(Props[SampleFinderActor]),"sampleFinder")
      map { case a: ActorRef => a}, 1 second)

  lazy val factExtractor = Await.result(
    supervisor ? CreateActor(BalancingPool(numberOfExtractorWorkers).props(Props[FreeTextFactExtractionActor]),"freetextExtractor")
      map { case a: ActorRef => a}, 1 second)

  lazy val statisticsExtractor = Await.result(
    supervisor ? CreateActor(BalancingPool(numberOfExtractorWorkers).props(Props[StatisticsCollectionActor]),"statisticsCollection")
      map { case a: ActorRef => a}, 1 second)

  lazy val sampleSaver = Await.result(
    supervisor ? CreateActor(Props[SampleSaverActor],"sampleSaver") map { case a: ActorRef => a}, 1 second)

  lazy val factSaver = Await.result(
    supervisor ? CreateActor(Props[FactSaverActor],"factSaver") map { case a: ActorRef => a}, 1 second)

  lazy val statisticsSaver = Await.result(
    supervisor ? CreateActor(Props[StatisticsSaverActor],"statSaver") map { case a: ActorRef => a}, 1 second)


  lazy val sampleCandidateSaver = Await.result(
    supervisor ? CreateActor(Props[SampleCandidateSaverActor],"sampleCandidteSaver") map { case a: ActorRef => a}, 1 second)


}
