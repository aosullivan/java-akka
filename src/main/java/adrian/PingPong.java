package adrian;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import scala.PartialFunction;
import scala.runtime.BoxedUnit;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.FI.UnitApply;
import akka.japi.pf.ReceiveBuilder;

/**
 * Run 100 simultaneous games of ping pong
 * 100 hits for each player per game
 */
public class PingPong {
    
    private static final int GAMES = 100;
    private static final int HITS = 100;
    
    static Map<Integer, Integer> pingTotals = new ConcurrentHashMap<>();
    static Map<Integer, Integer> pongTotals = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        PingPong game = new PingPong();
        game.begin();
    }

    private void begin() {
        ActorSystem system = ActorSystem.create("PPS");

        // Start 100 games
        IntStream.range(1, GAMES + 1)
            .forEach(game -> beginGame(game, system));
        
        System.out.println(pingTotals.toString());
        System.out.println(pongTotals.toString());
        
    }

    static class Ping extends AbstractActor {
        
        private ActorRef pong;
        int hits = 0;
        private Integer game;

        public Ping(ActorRef pong, Integer count) {
            System.err.println("ping constructor " + count);
            this.pong = pong;
            this.game = count;
        }

        @Override
        public PartialFunction<Object, BoxedUnit> receive() {

            System.err.println("Ping setup receivers " + game);

            UnitApply<PongMessage> pongHandler = message -> {
                pingTotals.compute(game, (k,v) -> v == null ? 0 : v+1);
                System.err.println("ping hit " + hits  + " game " + game + "      " + Thread.currentThread().getName());
                hits += 1;
                if (hits > HITS) {
                    sender().tell(new StopMessage(), self());
                    System.err.println("ping stopped " + game);
                    context().stop(self());
                } else {
                    sender().tell(new PingMessage(), self());
                }
            };

            UnitApply<StartMessage> startHandler = message -> {
                System.err.println("ping start " + game);
                pong.tell(new PingMessage(), self());
            };

            return ReceiveBuilder.match(StartMessage.class, startHandler)
                    .match(PongMessage.class, pongHandler).build();

        }

    }

    static class Pong extends AbstractActor {
        
        private Integer game;

        public Pong(Integer count) {
            System.err.println("pong constructor " + count);
            this.game = count;
        }

        @Override
        public PartialFunction<Object, BoxedUnit> receive() {

            System.err.println("Pong setup receivers " + game);

            UnitApply<PingMessage> pingHandler = message -> {
                pongTotals.compute(game, (k,v) -> v == null ? 0 : v+1);
                System.err.println("pong return, game " + game + "      " + Thread.currentThread().getName());
                sender().tell(new PongMessage(), self());
            };
            UnitApply<StopMessage> stopHandler = message -> {
                System.err.println("pong stopped " + game);
                context().stop(self());
            };

            return ReceiveBuilder.match(PingMessage.class, pingHandler)
                    .match(StopMessage.class, stopHandler).build();
        }
    }


    private void beginGame(Integer game, ActorSystem system) {
        ActorRef pong = system.actorOf(Props.create(Pong.class, game));
        ActorRef ping = system.actorOf(Props.create(Ping.class, pong, game));
        ping.tell(new StartMessage(), ActorRef.noSender());
    }
    
    static class PingMessage {
    };

    static class PongMessage {
    };

    static class StartMessage {
    };

    static class StopMessage {
    };
    

}
