package se.sics.ktoolbox.election.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Main Wrapper class for the serializer of the objects passed during promise phase
 * of the leader election.
 *
 * Created by babbar on 2015-04-15.
 */
public class PromiseSerializer {


    public static class Request implements Serializer {

        private final int id;

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
            
            Serializers.toBinary(request.id, byteBuf);
            Serializers.lookupSerializer(UUID.class).toBinary(request.electionRoundId, byteBuf);
            Serializers.toBinary(request.leaderAddress, byteBuf);
            Serializers.toBinary(request.leaderView, byteBuf);
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            Identifier eventId = (Identifier)Serializers.fromBinary(byteBuf, optional);
            UUID uuid = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            KAddress address = (KAddress)Serializers.fromBinary(byteBuf, optional);
            LCPeerView lcPeerView = (LCPeerView)Serializers.fromBinary(byteBuf, optional);
            
            return new Promise.Request(eventId, address, lcPeerView, uuid);
        }
    }

    public static class Response implements Serializer{
        private final int id;

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
            
            Serializers.toBinary(response.id, byteBuf);
            Serializers.lookupSerializer(UUID.class).toBinary(response.electionRoundId, byteBuf);
            byteBuf.writeBoolean(response.acceptCandidate);
            byteBuf.writeBoolean(response.isConverged);
            
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            Identifier eventId = (Identifier)Serializers.fromBinary(byteBuf, optional);
            UUID uuid = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            boolean acceptCandidate = byteBuf.readBoolean();
            boolean isConverged = byteBuf.readBoolean();
            
            return new Promise.Response(eventId, acceptCandidate, isConverged, uuid);
        }
    }
}
