package se.sics.ktoolbox.gradient.msg;

import java.util.Collection;
import se.sics.ktoolbox.gradient.event.GradientEvent;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * Wrapper class used by Gradient Service in order to encapsulate the Data
 * exchanged.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffle {

    public static abstract class Basic implements GradientEvent {

        public final Identifier id;
        public final GradientContainer selfGC;
        public final Collection<GradientContainer> exchangeNodes;
        
        public Basic(Identifier id, GradientContainer selfGC, Collection<GradientContainer> exchangeNodes) {
            this.id = id;
            this.selfGC = selfGC;
            this.exchangeNodes = exchangeNodes;
        }
    }
    
    public static class Request extends Basic {
        public Request(Identifier id, GradientContainer selfGC, Collection<GradientContainer> sample) {
            super(id, selfGC, sample);
        }
        
        @Override
        public String toString() {
            return "ShuffleRequest<" + getId() + ">";
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
    
    public static class Response extends Basic {
        public Response(Identifier id, GradientContainer selfGC, Collection<GradientContainer> sample) {
            super(id,  selfGC, sample);
        }
        
        @Override
        public String toString() {
            return "ShuffleResponse<" + getId() + ">";
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
}
