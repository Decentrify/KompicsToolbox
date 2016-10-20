package se.sics.ktoolbox.election.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.security.PublicKey;
import java.util.UUID;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistry;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Serializer for the extension request sent during the leader election
 * protocol.
 *
 * Created by babbar on 2015-04-15.
 */
public class ExtensionSerializer implements Serializer{

    private final int id;
    private final Class msgIdType;

    public ExtensionSerializer(int id){
        this.id = id;
        this.msgIdType = IdentifierRegistry.lookup(BasicIdentifiers.Values.MSG.toString()).idType();
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf byteBuf) {
        ExtensionRequest request = (ExtensionRequest)o;
        Serializers.lookupSerializer(msgIdType).toBinary(request.msgId, byteBuf);
        Serializers.toBinary(request.leaderAddress, byteBuf);
        Serializers.lookupSerializer(PublicKey.class).toBinary(request.leaderPublicKey, byteBuf);
        Serializers.lookupSerializer(UUID.class).toBinary(request.electionRoundId, byteBuf);
        Serializers.toBinary(request.leaderView, byteBuf);
    }

    @Override
    public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
        Identifier msgId = (Identifier)Serializers.lookupSerializer(msgIdType).fromBinary(byteBuf, optional);
        KAddress leaderAddress = (KAddress)Serializers.fromBinary(byteBuf, optional);
        PublicKey publicKey = (PublicKey)Serializers.lookupSerializer(PublicKey.class).fromBinary(byteBuf, optional);
        UUID electionRoundId = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(byteBuf, optional);
        LCPeerView lcpv = (LCPeerView)Serializers.fromBinary(byteBuf, optional);
        return new ExtensionRequest (msgId, leaderAddress, publicKey, lcpv, electionRoundId);
    }
}
