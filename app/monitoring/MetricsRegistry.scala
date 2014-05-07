package monitoring

import com.codahale.metrics.{Timer, ConsoleReporter}
import java.util.concurrent.TimeUnit

object MetricsRegistry {
  val metricRegistry = com.kenshoo.play.metrics.MetricsRegistry.default

  val reporter: ConsoleReporter = ConsoleReporter.forRegistry(metricRegistry)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .build();
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = MetricsRegistry.metricRegistry

}
