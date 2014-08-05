package actors

import scala.concurrent.duration._
import scala.concurrent.Await
import akka.actor.{ActorSystem, ActorRef, Props}
import akka.routing.RoundRobinRouter
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
    supervisor ? CreateActor(Props[ArticleDownloaderActor].withRouter(RoundRobinRouter(nrOfInstances = 3)),"wikipediaDownloader")
      map { case a: ActorRef => a}, 1 second)


  lazy val wikiMarkupToTextConverter = Await.result(
    supervisor ? CreateActor(Props[WikiMarkupToTextConverterActor].withRouter(RoundRobinRouter(nrOfInstances = numberOfExtractorWorkers)),"wikiMarkupToTextConverter")
      map { case a: ActorRef => a}, 1 second)


  lazy val infoboxExtractor = Await.result(
    supervisor ? CreateActor(Props[TemporalInfoboxExtractorActor].withRouter(RoundRobinRouter(nrOfInstances = numberOfExtractorWorkers)),"infoboxExtractor")
      map{ case a: ActorRef => a}, 20 seconds)


  lazy val sampleFinder = Await.result(
    supervisor ? CreateActor(Props[SampleFinderActor].withRouter(RoundRobinRouter(nrOfInstances = numberOfExtractorWorkers)),"sampleFinder")
      map { case a: ActorRef => a}, 1 second)

  lazy val sampleSaver = Await.result(
    supervisor ? CreateActor(Props[SampleSaverActor],"sampleSaver") map { case a: ActorRef => a}, 1 second)

  lazy val sampleCandidateSaver = Await.result(
    supervisor ? CreateActor(Props[SampleCandidateSaverActor],"sampleCandidteSaver") map { case a: ActorRef => a}, 1 second)


}
