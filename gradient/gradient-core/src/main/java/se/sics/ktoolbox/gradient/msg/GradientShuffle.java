package se.sics.ktoolbox.gradient.msg;

import java.util.List;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.gradient.event.GradientEvent;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;

/**
 * Wrapper class used by Gradient Service in order to encapsulate the Data
 * exchanged.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffle {

    public static abstract class Basic implements GradientEvent {

        public final Identifier msgId;
        public final OverlayId overlayId;
        public final GradientContainer selfGC;
        public final List<GradientContainer> exchangeGC;
        
        public Basic(Identifier msgId, OverlayId overlayId,
                GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            this.msgId = msgId;
            this.overlayId = overlayId;
            this.selfGC = selfGC;
            this.exchangeGC = exchangeGC;
        }
        
        public Basic(OverlayId overlayId, GradientContainer selfGC, List<GradientContainer> exchangeNodes) {
            this(BasicIdentifiers.msgId(), overlayId, selfGC, exchangeNodes);
        }
        
        @Override
        public OverlayId overlayId() {
            return overlayId;
        }
        
        @Override
        public Identifier getId() {
            return msgId;
        }
    }
    
    public static class Request extends Basic {
        public Request(Identifier msgId, OverlayId overlayId, 
                GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            super(msgId, overlayId, selfGC, exchangeGC);
        }
        
        public Request(OverlayId overlayId, 
                GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            super(overlayId, selfGC, exchangeGC);
        }
        
        @Override
        public String toString() {
            return "Gradient<" + overlayId() + ">ShuffleReq<" + getId() + ">";
        }
        
        public Response answer(GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            return new Response(msgId, overlayId, selfGC, exchangeGC);
        }
    }
    
    public static class Response extends Basic {
        public Response(Identifier msgId, OverlayId overlayId, GradientContainer selfGC, 
                List<GradientContainer> exchangeGC) {
            super(msgId, overlayId, selfGC, exchangeGC);
        }
        
        public Response(OverlayId overlayId, GradientContainer selfGC, List<GradientContainer> exchangeGC) {
            super(overlayId, selfGC, exchangeGC);
        }
        
        @Override
        public String toString() {
            return "Gradient<" + overlayId() + ">ShuffleResp<" + getId() + ">";
        }
    }
}
