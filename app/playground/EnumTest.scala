package playground

import models.EventTypes
import models.EventTypes.EventType

/**
 * Created by Norman on 06.04.14.
 */
object EnumTest extends App {

  val eventtype = EventTypes.withNameOpt("exception")
  println(eventtype.get.description)

}
