package se.sics.p2ptoolbox.gradient.msg;

import com.google.common.base.Objects;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
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
public class ShuffleNet {

    public static class Request extends NetContentMsg.Request<Shuffle> {

        public Request(VodAddress src, VodAddress dest, UUID id, int overlayId, Shuffle content) {
            super(src, dest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public Request(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header, Shuffle content) {
            super(src, dest, id, header, content);
        }

        @Override
        public RewriteableMsg copy() {
            return new Request(vodSrc, vodDest, id, header, content);
        }

        public String toString() {
            return "GRADIENT_SHUFFLE_NET_REQUEST";
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
            final Request that = (Request) obj;
            if(!Objects.equal(this.id, that.id)) {
                return false;
            }
            if (!Objects.equal(this.content, that.content)) {
                return false;
            }
            if (!Objects.equal(this.header, that.header)) {
                return false;
            }
            if (!Objects.equal(this.vodSrc, that.vodSrc)) {
                return false;
            }
            if (!Objects.equal(this.vodDest, that.vodDest)) {
                return false;
            }
            return true;
        }
    }

    public static class Response extends NetContentMsg.Response<Shuffle> {

        public Response(VodAddress src, VodAddress dest, UUID id, int overlayId, Shuffle content) {
            super(src, dest, id, content);
            header.put("overlay", new OverlayHeaderField(overlayId));
        }

        public Response(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header, Shuffle content) {
            super(src, dest, id, header, content);
        }

        @Override
        public String toString() {
            return "GRADIENT_SHUFFLE_NET_RESPONSE";
        }

        @Override
        public Response copy() {
            return new Response(vodSrc, vodDest, id, header, content);
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
            final Response that = (Response) obj;
            if(!Objects.equal(this.id, that.id)) {
                return false;
            }
            if (!Objects.equal(this.content, that.content)) {
                return false;
            }
            if (!Objects.equal(this.header, that.header)) {
                return false;
            }
            if (!Objects.equal(this.vodSrc, that.vodSrc)) {
                return false;
            }
            if (!Objects.equal(this.vodDest, that.vodDest)) {
                return false;
            }
            return true;
        }
    }
}
