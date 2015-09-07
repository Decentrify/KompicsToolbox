package se.sics.ktoolbox.aggregator.global.network;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.aggregator.global.api.system.PacketContainer;
import se.sics.ktoolbox.aggregator.global.api.system.PacketInfo;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.UUID;

/**
 * Serializer for the packet container mainly containing the
 * packet information.
 * Created by babbar on 2015-09-07.
 */
public class PacketContainerSerializer implements Serializer {

    private int id;

    public PacketContainerSerializer(int id){
        this.id = id;
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {

        PacketContainer container = (PacketContainer)o;
        Serializers.lookupSerializer(UUID.class).toBinary(container.uuid, buf);
        Serializers.lookupSerializer(DecoratedAddress.class).toBinary(container.sourceAddress, buf);
        Serializers.toBinary(container.packetInfo, buf);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {


        UUID uuid = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
        DecoratedAddress selfAddress = (DecoratedAddress)Serializers.lookupSerializer(DecoratedAddress.class).fromBinary(buf, hint);
        PacketInfo packetInfo = (PacketInfo)Serializers.fromBinary(buf, hint);

        return new PacketContainer(uuid, selfAddress, packetInfo);
    }
}
