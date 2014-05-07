package actors

import scala.concurrent.duration._
import scala.concurrent.Await
import akka.actor.{ActorRef, Props}
import akka.routing.{DefaultResizer, RoundRobinRouter}
import akka.util.Timeout
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.Play.current

import actors.supervision.MteSupervisor



object DefaultActors {

  import actors._
  implicit val timeout = Timeout(20 second)

  lazy val supervisor = Akka.system.actorOf(Props[MteSupervisor])

  lazy val dbPedia = Await.result( supervisor ? Props[DBpediaSparqlEndpointActor] map{ case a: ActorRef => a}, 1 second)

  lazy val pageDownloader = Await.result(
    supervisor ? Props[ArticleDownloaderActor].withRouter(RoundRobinRouter(nrOfInstances = 3))
      map{ case a: ActorRef => a}, 1 second)

  lazy val wikiMarkupToTextConverter = Await.result(
    supervisor ? Props[WikiMarkupToTextConverterActor].withRouter(RoundRobinRouter(nrOfInstances = 6))
      map{ case a: ActorRef => a}, 1 second)


  lazy val sampleFinder = Await.result(
    supervisor ? Props[SampleFinderActor].withRouter(RoundRobinRouter(nrOfInstances = 6))
      map{ case a: ActorRef => a}, 1 second)



  private val resizer = DefaultResizer(lowerBound = 1, upperBound = 6, messagesPerResize = 5)
  private val router = RoundRobinRouter(nrOfInstances = 6).withResizer(resizer)
  lazy val infoboxExtractor = Await.result(
    supervisor ? Props[TemporalInfoboxExtractorActor].withRouter(router)
      map{ case a: ActorRef => a}, 10 seconds)


}
