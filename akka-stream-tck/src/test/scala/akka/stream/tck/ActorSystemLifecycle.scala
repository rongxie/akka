/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream.tck

import java.util.concurrent.TimeoutException
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.ActorSystemImpl
import org.testng.annotations.AfterClass
import akka.stream.testkit.AkkaSpec
import akka.event.Logging
import akka.testkit.TestEvent
import akka.testkit.EventFilter

trait ActorSystemLifecycle {

  /**
   * Implement this in the concrete test by:
   * {{
   * override lazy val system = createActorSystem()
   * }}
   *
   * TestNG creates 2 instances of the test class and one of
   * them is not used and @AfterClass is not invoked on one of
   * the instances, i.e. the system is not shutdown.
   */
  def system: ActorSystem

  def shutdownTimeout: FiniteDuration = 10.seconds

  def createActorSystem(): ActorSystem = {
    val sys = ActorSystem(Logging.simpleName(getClass), AkkaSpec.testConf)
    sys.eventStream.publish(TestEvent.Mute(EventFilter[RuntimeException]("Test exception")))
    sys
  }

  @AfterClass
  def shutdownActorSystem(): Unit = {
    try {
      system.shutdown()
      system.awaitTermination(shutdownTimeout)
    } catch {
      case _: TimeoutException ⇒
        val msg = "Failed to stop [%s] within [%s] \n%s".format(system.name, shutdownTimeout,
          system.asInstanceOf[ActorSystemImpl].printTree)
        throw new RuntimeException(msg)
    }
  }

}
