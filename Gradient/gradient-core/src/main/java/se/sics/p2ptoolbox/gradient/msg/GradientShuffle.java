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

        protected final UUID id;
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
        
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 23 * hash + (this.selfGC != null ? this.selfGC.hashCode() : 0);
            hash = 23 * hash + (this.exchangeNodes != null ? this.exchangeNodes.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Request other = (Request) obj;
            if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
                return false;
            }
            if (this.selfGC != other.selfGC && (this.selfGC == null || !this.selfGC.equals(other.selfGC))) {
                return false;
            }
            if (this.exchangeNodes != other.exchangeNodes && (this.exchangeNodes == null || !this.exchangeNodes.equals(other.exchangeNodes))) {
                return false;
            }
            return true;
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
        
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + (this.id != null ? this.id.hashCode() : 0);
            hash = 23 * hash + (this.selfGC != null ? this.selfGC.hashCode() : 0);
            hash = 23 * hash + (this.exchangeNodes != null ? this.exchangeNodes.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Response other = (Response) obj;
            if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
                return false;
            }
            if (this.selfGC != other.selfGC && (this.selfGC == null || !this.selfGC.equals(other.selfGC))) {
                return false;
            }
            if (this.exchangeNodes != other.exchangeNodes && (this.exchangeNodes == null || !this.exchangeNodes.equals(other.exchangeNodes))) {
                return false;
            }
            return true;
        }
    }
}
