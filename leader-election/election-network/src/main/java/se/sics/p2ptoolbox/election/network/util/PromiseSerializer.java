package se.sics.p2ptoolbox.election.network.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.core.data.Promise;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.UUID;

/**
 * Main Wrapper class for the serializer of the objects passed during promise phase
 * of the leader election.
 *
 * Created by babbar on 2015-04-15.
 */
public class PromiseSerializer {


    public static class Request implements Serializer {

        private int id;

        public Request(int id){
            this.id = id;
        }

        @Override
        public int identifier() {
            return this.id;
        }

        @Override
        public void toBinary(Object o, ByteBuf byteBuf) {
            
            Promise.Request request = (Promise.Request)o;
            Serializers.lookupSerializer(UUID.class).toBinary(request.electionRoundId, byteBuf);
            Serializers.lookupSerializer(DecoratedAddress.class).toBinary(request.leaderAddress, byteBuf);
            Serializers.toBinary(request.leaderView, byteBuf);
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            
            UUID uuid = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            DecoratedAddress address = (DecoratedAddress)Serializers.lookupSerializer(DecoratedAddress.class).fromBinary(byteBuf, optional);
            LCPeerView lcPeerView = (LCPeerView)Serializers.fromBinary(byteBuf, optional);
            
            return new Promise.Request(address, lcPeerView, uuid);
        }
    }


    public static class Response implements Serializer{


        private int id;

        public Response(int id){
            this.id = id;
        }

        @Override
        public int identifier() {
            return this.id;
        }

        @Override
        public void toBinary(Object o, ByteBuf byteBuf) {
            
            Promise.Response response = (Promise.Response)o;
            Serializers.lookupSerializer(UUID.class).toBinary(response.electionRoundId, byteBuf);
            byteBuf.writeBoolean(response.acceptCandidate);
            byteBuf.writeBoolean(response.isConverged);
            
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            
            UUID uuid = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            boolean acceptCandidate = byteBuf.readBoolean();
            boolean isConverged = byteBuf.readBoolean();
            
            return new Promise.Response(acceptCandidate, isConverged, uuid);
        }
    }


}
