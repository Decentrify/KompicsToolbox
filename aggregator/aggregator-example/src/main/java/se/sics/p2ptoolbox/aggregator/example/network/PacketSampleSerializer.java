package se.sics.p2ptoolbox.aggregator.example.network;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.p2ptoolbox.aggregator.example.core.PacketSample;

/**
 * Serializer for the Packet Sample {@link se.sics.p2ptoolbox.aggregator.example.core.PacketSample}
 * Created by babbarshaer on 2015-03-17.
 */
public class PacketSampleSerializer implements Serializer{

    int id;

    public PacketSampleSerializer(int id){
        this.id = id;
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buffer) {

        PacketSample obj = (PacketSample)o;

        buffer.writeInt(obj.nodeId);
        buffer.writeInt(obj.partitionId);
        buffer.writeInt(obj.partitionDepth);
        buffer.writeInt(obj.indexEntries);

    }

    @Override
    public Object fromBinary(ByteBuf buffer, Optional<Object> optional) {

        int nodeId = buffer.readInt();
        int partitionId = buffer.readInt();
        int partitionDepth = buffer.readInt();
        int indexEntries = buffer.readInt();

        return new PacketSample(partitionDepth, indexEntries, partitionId, nodeId);
    }
}
