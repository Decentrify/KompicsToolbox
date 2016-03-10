package se.sics.ktoolbox.gradient.msg;

import java.util.Collection;
import java.util.List;
import se.sics.ktoolbox.gradient.event.GradientEvent;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

/**
 * Wrapper class used by Gradient Service in order to encapsulate the Data
 * exchanged.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffle {

    public static abstract class Basic implements GradientEvent {

        public final Identifier eventId;
        public final Identifier overlayId;
        public final GradientContainer selfGC;
        public final List<GradientContainer> exchangeGC;
        
        public Basic(Identifier eventId, Identifier overlayId,
                GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            this.eventId = eventId;
            this.overlayId = overlayId;
            this.selfGC = selfGC;
            this.exchangeGC = exchangeGC;
        }
        
        public Basic(Identifier overlayId, GradientContainer selfGC, List<GradientContainer> exchangeNodes) {
            this(UUIDIdentifier.randomId(), overlayId, selfGC, exchangeNodes);
        }
        
        @Override
        public Identifier overlayId() {
            return overlayId;
        }
        
        @Override
        public Identifier getId() {
            return eventId;
        }
    }
    
    public static class Request extends Basic {
        public Request(Identifier eventId, Identifier overlayId, 
                GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            super(eventId, overlayId, selfGC, exchangeGC);
        }
        
        public Request(Identifier overlayId, 
                GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            super(overlayId, selfGC, exchangeGC);
        }
        
        @Override
        public String toString() {
            return "Gradient<" + overlayId() + ">ShuffleReq<" + getId() + ">";
        }
        
        public Response answer(GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            return new Response(eventId, overlayId, selfGC, exchangeGC);
        }
    }
    
    public static class Response extends Basic {
        public Response(Identifier eventId, Identifier overlayId, GradientContainer selfGC, 
                List<GradientContainer> exchangeGC) {
            super(eventId, overlayId, selfGC, exchangeGC);
        }
        
        public Response(Identifier overlayId, GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            super(overlayId, selfGC, exchangeGC);
        }
        
        @Override
        public String toString() {
            return "Gradient<" + overlayId() + ">ShuffleResp<" + getId() + ">";
        }
    }
}
