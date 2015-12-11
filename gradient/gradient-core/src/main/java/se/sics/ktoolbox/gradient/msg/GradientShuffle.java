package se.sics.ktoolbox.gradient.msg;

import java.util.Collection;
import java.util.UUID;
import se.sics.ktoolbox.gradient.util.GradientContainer;

/**
 * Wrapper class used by Gradient Service in order to encapsulate the Data
 * exchanged.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffle {

    public static abstract class Basic implements GradientMsg {

        public final UUID id;
        public final GradientContainer selfGC;
        public final Collection<GradientContainer> exchangeNodes;
        
        public Basic(UUID id, GradientContainer selfGC, Collection<GradientContainer> exchangeNodes) {
            this.id = id;
            this.selfGC = selfGC;
            this.exchangeNodes = exchangeNodes;
        }
    }
    
    public static class Request extends Basic {
        public Request(UUID id, GradientContainer selfGC, Collection<GradientContainer> sample) {
            super(id, selfGC, sample);
        }
        
        @Override
        public String toString() {
            return "ShuffleRequest";
        }
        
    }
    
    public static class Response extends Basic {
        public Response(UUID id, GradientContainer selfGC, Collection<GradientContainer> sample) {
            super(id,  selfGC, sample);
        }
        
        @Override
        public String toString() {
            return "ShuffleResponse";
        }
    }
}
