package adrian;

import scala.PartialFunction;
import scala.runtime.BoxedUnit;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.FI.UnitApply;
import akka.japi.pf.ReceiveBuilder;

public class PingPong {
	
	static class PingMessage{};
	static class PongMessage{};
	static class StartMessage{};
	static class StopMessage{};	
	
	static class Ping extends AbstractActor {
		
		private ActorRef pong;
		int count = 0;
		
		public Ping(ActorRef pong) {
			System.err.println("ping constructor");
			this.pong = pong;
		}

		@Override
		public PartialFunction<Object, BoxedUnit> receive() {
			
			System.err.println("Ping receive");
			
			UnitApply<PongMessage> pongHandler = message -> { System.err.println("ping " + count + ' ' + Thread.currentThread().getName());
			  										 count += 1;
			  										 if (count > 1025) {
			  											sender().tell(new StopMessage(), self());
			  											System.err.println("ping stopped");
			  								            context().stop(self());
			  										 } else {
			  											 sender().tell(new PingMessage(), self());             									   
			  										 }};
			  										 
			UnitApply<StartMessage> startHandler = message -> { System.err.println("ping start");
			  										  count += 1;
			  										  pong.tell(new PingMessage(), self()); };
			  									      	
			return ReceiveBuilder
		              .match(StartMessage.class, startHandler)
		              .match(PongMessage.class, pongHandler)		              										 
		              .build();
			
		}
		
	}
	
	static class Pong extends AbstractActor {
		
		@Override
		public PartialFunction<Object, BoxedUnit> receive() {
			
			System.err.println("Pong receive");
			
			UnitApply<PingMessage> pingHandler = message -> { System.err.println("pong " + Thread.currentThread().getName());
              									     sender().tell(new PongMessage(), self()); };
			UnitApply<StopMessage> stopHandler = message -> { System.err.println("pong stopped");
              									     context().stop(self()); };
              									     
			return ReceiveBuilder
              .match(PingMessage.class, pingHandler )
              .match(StopMessage.class, stopHandler )              									   
              .build();
		}
	}
	
	public static void main(String[] args){
		PingPong game = new PingPong();
		game.begin();
	}

	private void begin() {
	  ActorSystem system = ActorSystem.create("PingPongSystem");
	  
	  ActorRef pong = system.actorOf(Props.create(Pong.class));
	  ActorRef ping = system.actorOf(Props.create(Ping.class, pong));
	  
	  // start them going
	  ping.tell(new StartMessage(), ActorRef.noSender());
	}

	

	
        
}
