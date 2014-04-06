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

  lazy val dbPedia = Await.result( supervisor ? Props[DBpediaActor] map{ case a: ActorRef => a}, 1 second)

  lazy val pageDownloader = Await.result(
    supervisor ? Props[PageDownloaderActor].withRouter(RoundRobinRouter(nrOfInstances = 3))
      map{ case a: ActorRef => a}, 1 second)


  private val resizer = DefaultResizer(lowerBound = 1, upperBound = 8, messagesPerResize = 5)
  private val router = RoundRobinRouter(nrOfInstances = 8).withResizer(resizer)
  lazy val infoboxExtractor = Await.result(
    supervisor ? Props[TemporalInfoboxExtractorActor].withRouter(router)
      map{ case a: ActorRef => a}, 1 second)


}
