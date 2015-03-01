package se.sics.p2ptoolbox.gradient.msg;

import com.google.common.base.Objects;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.p2ptoolbox.gradient.core.GradientShuffle;
import se.sics.p2ptoolbox.serialization.msg.HeaderField;
import se.sics.p2ptoolbox.serialization.msg.NetContentMsg;
import se.sics.p2ptoolbox.serialization.msg.OverlayHeaderField;

import java.util.Map;
import java.util.UUID;

/**
 * Network Message encapsulating the Gradient Message Payload.
 *  
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffleNet {

    public static class Request extends NetContentMsg.Request<GradientShuffle>{


        public Request(VodAddress src, VodAddress dest, UUID id, int overlayId, Map<String, HeaderField> header, GradientShuffle content) {
            super(src, dest, id, header, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public Request(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, GradientShuffle content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new Request(vodSrc, vodDest, id, header, (GradientShuffle) content);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.content);
            hash = 37 * hash + Objects.hashCode(this.header);
            hash = 37 * hash + Objects.hashCode(this.vodSrc);
            hash = 37 * hash + Objects.hashCode(this.vodDest);
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
            if (!Objects.equal(this.content, other.content)) {
                return false;
            }
            if (!Objects.equal(this.header, other.header)) {
                return false;
            }
            if (!Objects.equal(this.vodSrc, other.vodSrc)) {
                return false;
            }
            if (!Objects.equal(this.vodDest, other.vodDest)) {
                return false;
            }
            return true;
        }
         
        public String toString(){
            return "GRADIENT_SHUFFLE_NET_REQUEST" + " src " + vodSrc.getPeerAddress() + " dest " + vodDest.getPeerAddress();
        }
    }
    
    
    public static class Response extends NetContentMsg.Response<GradientShuffle>{


        public Response(VodAddress src, VodAddress dest, UUID id, int overlayId, GradientShuffle content) {
            super(src, dest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public Response(VodAddress vodSrc, VodAddress vodDest, UUID id, Map<String, HeaderField> header, GradientShuffle content) {
            super(vodSrc, vodDest, id, header, content);
        }

        @Override
        public String toString() {
            return "GRADIENT_SHUFFLE_NET_RESPONSE" + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }

        @Override
        public Response copy() {
            return new Response(vodSrc, vodDest, id, header, (GradientShuffle) content);
        }


        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.content);
            hash = 37 * hash + Objects.hashCode(this.header);
            hash = 37 * hash + Objects.hashCode(this.vodSrc);
            hash = 37 * hash + Objects.hashCode(this.vodDest);
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
            if (!Objects.equal(this.content, other.content)) {
                return false;
            }
            if (!Objects.equal(this.header, other.header)) {
                return false;
            }
            if (!Objects.equal(this.vodSrc, other.vodSrc)) {
                return false;
            }
            if (!Objects.equal(this.vodDest, other.vodDest)) {
                return false;
            }
            return true;
        }
        
    }
    
    
}
