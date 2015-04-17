package se.sics.p2ptoolbox.aggregator.network.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.UUID;

/**
 * Serializer the aggregated state being sent on the network.
 *
 * Created by babbar on 2015-04-15.
 */
public class AggregatedStateContainerSerializer implements Serializer{

    private int id;

    public AggregatedStateContainerSerializer(int id){
        this.id = id;
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {

        AggregatedStateContainer container = (AggregatedStateContainer)o;

        Serializers.lookupSerializer(UUID.class).toBinary(container.getId(), buf);
        Serializers.lookupSerializer(DecoratedAddress.class).toBinary(container.getAddress(), buf);
        Serializers.toBinary(container.getPacketInfo(), buf);

    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> optional) {

        UUID uuid = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(buf, optional);
        DecoratedAddress address  = (DecoratedAddress)Serializers.lookupSerializer(DecoratedAddress.class).fromBinary(buf, optional);
        AggregatedStatePacket packetInfo = (AggregatedStatePacket)Serializers.fromBinary(buf, optional);

        return new AggregatedStateContainer(uuid, address, packetInfo);
    }
}
