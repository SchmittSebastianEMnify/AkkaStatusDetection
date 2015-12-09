package actors;

import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Identify;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.remote.DisassociatedEvent;
import msgs.Reconnect;
import scala.concurrent.duration.Duration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SenderActor extends UntypedActor {

  long reconnectTime;
  long identityId = 1;
  private Map<Long, String> identities = new HashMap<>();

  String receiverPath;
  ActorRef receiver;
  Timer scheduler;

  public SenderActor() {
    System.out.println("Starting SenderActor " + getSelf().path().toString());

    receiverPath = getContext().system().settings().config().getString("sender.receiver");
    reconnectTime = getContext().system().settings().config().getLong("sender.receiver-retry");

    sendIdentify(receiverPath);

    getContext().system().eventStream().subscribe(getSelf(), DisassociatedEvent.class);
  }

  @Override
  public void onReceive(Object msg) throws Exception {
    if (msg instanceof Reconnect) {

      sendIdentify(receiverPath);

    } else if (msg instanceof DisassociatedEvent) {

      if (receiver != null) {
        System.out.println("Received DISASSOCIATEDEVENT");
        DisassociatedEvent de = (DisassociatedEvent) msg;
        if (receiverPath.startsWith(de.getRemoteAddress().toString())) {
          cancelScheduler();
          receiver = null;
          reconnect();
        }
      }

    } else if (msg instanceof Terminated) {

      if (receiver != null) {
        System.out.println("Received TERMINATED");
        Terminated ter = (Terminated) msg;
        if (receiverPath.startsWith(ter.getActor().path().toString())) {
          cancelScheduler();
          receiver = null;
          reconnect();
        }
      }

    } else if (msg instanceof ActorIdentity) {

      onActorIdentity((ActorIdentity) msg);

    }

  }

  private void sendIdentify(String path) {
    String fullpath = path + "/user/receiver";
    System.out.println("Send 'Identify' to " + fullpath);
    long idId = identityId++;
    ActorSelection actor = getContext().actorSelection(fullpath);
    identities.put(idId, path);
    actor.tell(new Identify(idId), getSelf());
  }

  private void onActorIdentity(ActorIdentity msg) {
    if (receiver == null) {
      String path = identities.remove(msg.correlationId());
      if (path == null) {
        System.out.println("Unexpected query response, queryId=" + msg.correlationId());
        return;
      }
      ActorRef ref = msg.getRef();
      if (ref == null) {
        reconnect();
        return;
      }

      System.out.println("Connected to Receiver " + ref.path());
      receiver = ref;
      try {
        scheduler.cancel();
      } catch (Exception e) {
      }
      schedule();
      getContext().watch(ref);
    }
  }

  private void reconnect() {
    getContext()
        .system()
        .scheduler()
        .scheduleOnce(Duration.create(reconnectTime, TimeUnit.SECONDS), getSelf(), new Reconnect(),
            getContext().dispatcher(), null);
  }

  private void schedule() {
    if (scheduler == null) {
      scheduler = new Timer();
      scheduler.schedule(new TimerTask() {
        @Override
        public void run() {
          System.out.println("Sending");
          receiver.tell(new Date(), getSelf());
        }
      }, 0, 1000);
    }
  }

  private void cancelScheduler() {
    try {
      scheduler.cancel();
      scheduler = null;
    } catch (Exception e) {
    }
  }

  public static Props props() {
    return Props.create(SenderActor.class);
  }
}
