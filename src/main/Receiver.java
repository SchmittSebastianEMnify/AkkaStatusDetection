package main;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import actors.ReceiverActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;

public class Receiver implements Bootable {
  final Config config = ConfigFactory.load("receiver");
  final ActorSystem actorSystem = ActorSystem.create("test", config);

  @Override
  public void startup() {
    actorSystem.actorOf(Props.create(ReceiverActor.class), "receiver");
  }

  @Override
  public void shutdown() {
    actorSystem.shutdown();
  }
}
