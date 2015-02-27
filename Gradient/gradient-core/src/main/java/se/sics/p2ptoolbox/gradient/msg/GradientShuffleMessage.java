package se.sics.p2ptoolbox.gradient.msg;

import io.netty.buffer.ByteBuf;
import se.sics.co.IndividualTimeout;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.TimeoutId;
import se.sics.p2ptoolbox.gradient.api.util.GradientPeerView;

import java.util.Set;

/**
 * Gradient Shuffle Message Wrapper.
 * 
 * Created by babbarshaer on 2015-02-27.
 */
public class GradientShuffleMessage {

    /**
     * TODO: Need to send the gradient peer view along with this.
     * Gradient Shuffle Request 
     */
    public static class Request extends DirectMsgNetty.Request{

        private final Set<GradientPeerView> exchangeNodes;
        
        public Request(VodAddress source, VodAddress destination, TimeoutId timeout, Set<GradientPeerView> exchangeNodes) {
            super(source, destination, timeout);
            this.exchangeNodes = exchangeNodes;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public RewriteableMsg copy() {
            return null;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            return null;
        }

        @Override
        public byte getOpcode() {
            return 0;
        }

        public Set<GradientPeerView> getExchangeNodes() {
            return exchangeNodes;
        }
    }


    /**
     * Gradient Shuffle Message Response.
     */
    public static class Response extends DirectMsgNetty.Response{

        private Set<GradientPeerView> exchangeNodes;
        
        public Response(VodAddress source, VodAddress destination,TimeoutId timeoutId, Set<GradientPeerView> exchangeNodes) {
            super(source, destination, timeoutId);
            this.exchangeNodes = exchangeNodes;
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public RewriteableMsg copy() {
            return null;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            return null;
        }

        @Override
        public byte getOpcode() {
            return 0;
        }

        public Set<GradientPeerView> getExchangeNodes() {
            return exchangeNodes;
        }
    }
    
    
    public static class Timeout extends IndividualTimeout{

        public Timeout(ScheduleTimeout request, int id) {
            super(request, id);
        }
    }
}
