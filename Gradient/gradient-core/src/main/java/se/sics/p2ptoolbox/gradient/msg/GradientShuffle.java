package se.sics.p2ptoolbox.gradient.msg;

import java.util.Set;
import java.util.UUID;
import se.sics.p2ptoolbox.gradient.util.GradientContainer;
import se.sics.p2ptoolbox.util.identifiable.UUIDIdentifiable;

/**
 * Wrapper class used by Gradient Service in order to encapsulate the Data
 * exchanged.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffle {

    public static abstract class Basic implements UUIDIdentifiable {

        private final UUID id;
        public final GradientContainer selfGC;
        public final Set<GradientContainer> exchangeNodes;
        
        public Basic(UUID id, GradientContainer selfGC, Set<GradientContainer> exchangeNodes) {
            this.id = id;
            this.selfGC = selfGC;
            this.exchangeNodes = exchangeNodes;
        }
        
        @Override
        public final UUID getId() {
            return id;
        }
    }
    
    public static class Request extends Basic {
        public Request(UUID id, GradientContainer selfGC, Set<GradientContainer> sample) {
            super(id, selfGC, sample);
        }
        
        @Override
        public String toString() {
            return "ShuffleRequest";
        }
    }
    
    public static class Response extends Basic {
        public Response(UUID id, GradientContainer selfGC, Set<GradientContainer> sample) {
            super(id,  selfGC, sample);
        }
        
        @Override
        public String toString() {
            return "ShuffleResponse";
        }
    }
}
