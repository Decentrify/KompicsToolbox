package se.sics.ktoolbox.election.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.security.PublicKey;
import java.util.UUID;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Serializer for the lease commit message during the leader election protocol.
 *
 * Created by babbar on 2015-04-15.
 */
public class LeaseCommitSerializer {
    public static class Request implements Serializer{

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
            LeaseCommitUpdated.Request request= (LeaseCommitUpdated.Request)o;
            Serializers.toBinary(request.id, byteBuf);
            Serializers.toBinary(request.leaderAddress, byteBuf);
            Serializers.lookupSerializer(PublicKey.class).toBinary(request.leaderPublicKey, byteBuf);
            Serializers.lookupSerializer(UUID.class).toBinary(request.electionRoundId, byteBuf);
            Serializers.toBinary(request.leaderView, byteBuf);
            
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            Identifier eventId = (Identifier)Serializers.fromBinary(byteBuf, optional);
            KAddress leaderAddress = (KAddress)Serializers.fromBinary(byteBuf, optional);
            PublicKey publicKey = (PublicKey)Serializers.lookupSerializer(PublicKey.class).fromBinary(byteBuf, optional);
            UUID electionRoundId = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            LCPeerView lcpv = (LCPeerView)Serializers.fromBinary(byteBuf, optional);
            
            return new LeaseCommitUpdated.Request(eventId, leaderAddress, publicKey, lcpv, electionRoundId);
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
            LeaseCommitUpdated.Response response = (LeaseCommitUpdated.Response)o;
            
            Serializers.toBinary(response.id, byteBuf);
            Serializers.lookupSerializer(UUID.class).toBinary(response.electionRoundId, byteBuf);
            byteBuf.writeBoolean(response.isCommit);
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            Identifier eventId = (Identifier)Serializers.fromBinary(byteBuf, optional);
            UUID electionRoundId = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            boolean isCommit = byteBuf.readBoolean();
            
            return new LeaseCommitUpdated.Response(eventId, isCommit, electionRoundId);
        }
    }
}
