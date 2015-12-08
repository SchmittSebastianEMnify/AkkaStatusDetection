package main;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import actors.SenderActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;

public class Sender implements Bootable {
  final Config config = ConfigFactory.load("sender");
  final ActorSystem system = ActorSystem.create("test", config);

  @Override
  public void startup() {
    system.actorOf(Props.create(SenderActor.class), "sender");
  }

  @Override
  public void shutdown() {
    system.shutdown();
  }
}
