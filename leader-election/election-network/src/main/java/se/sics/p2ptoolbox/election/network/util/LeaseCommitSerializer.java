package se.sics.p2ptoolbox.election.network.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.core.data.LeaseCommitUpdated;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.security.PublicKey;
import java.util.UUID;

/**
 * Serializer for the lease commit message during the leader election protocol.
 *
 * Created by babbar on 2015-04-15.
 */
public class LeaseCommitSerializer {



    public static class Request implements Serializer{

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
            
            LeaseCommitUpdated.Request request= (LeaseCommitUpdated.Request)o;
            Serializers.lookupSerializer(DecoratedAddress.class).toBinary(request.leaderAddress, byteBuf);
            Serializers.lookupSerializer(PublicKey.class).toBinary(request.leaderPublicKey, byteBuf);
            Serializers.lookupSerializer(UUID.class).toBinary(request.electionRoundId, byteBuf);
            Serializers.toBinary(request.leaderView, byteBuf);
            
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {

            DecoratedAddress leaderAddress = (DecoratedAddress)Serializers.lookupSerializer(DecoratedAddress.class).fromBinary(byteBuf, optional);
            PublicKey publicKey = (PublicKey)Serializers.lookupSerializer(PublicKey.class).fromBinary(byteBuf, optional);
            UUID electionRoundId = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            LCPeerView lcpv = (LCPeerView)Serializers.fromBinary(byteBuf, optional);
            
            return new LeaseCommitUpdated.Request(leaderAddress, publicKey, lcpv, electionRoundId);
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
            
            LeaseCommitUpdated.Response response = (LeaseCommitUpdated.Response)o;
            Serializers.lookupSerializer(UUID.class).toBinary(response.electionRoundId, byteBuf);
            byteBuf.writeBoolean(response.isCommit);
        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            
            UUID electionRoundId = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
            boolean isCommit = byteBuf.readBoolean();
            
            return new LeaseCommitUpdated.Response(isCommit, electionRoundId);
        }
    }

}
