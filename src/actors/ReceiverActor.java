package actors;

import java.util.Date;

import akka.actor.UntypedActor;

public class ReceiverActor extends UntypedActor {

  public ReceiverActor() {
    System.out.println("Starting ReceiverActor " + getSelf().path().toString());
  }

  @Override
  public void onReceive(Object msg) throws Exception {
    if (msg instanceof Date) {
      System.out.println(msg.toString());
    }

  }

}
