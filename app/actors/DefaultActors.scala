package actors

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import play.api.Play.current
import play.api.libs.concurrent.Akka
import actors.supervision.MteSupervisor
import scala.concurrent.Await
import akka.routing.RoundRobinRouter


/**
 * Created by Norman on 31.03.14.
 */
object DefaultActors {

  import actors._
  implicit val timeout = Timeout(20 second)

  val supervisor = Akka.system.actorOf(Props[MteSupervisor])

  val dbPedia = Await.result( supervisor ? Props[DBpediaActor] map{ case a: ActorRef => a}, 1 second)

  val pageDownloader = Await.result(
    supervisor ? Props[PageDownloaderActor].withRouter(RoundRobinRouter(nrOfInstances = 3))
      map{ case a: ActorRef => a}, 1 second)


}
