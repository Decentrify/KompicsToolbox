package se.sics.p2ptoolbox.aggregator.example.core;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * Created by babbarshaer on 2015-03-17.
 */
public class PacketSampleSerializer implements Serializer<PacketSample>{
    
    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buffer, PacketSample obj) throws SerializerException, SerializationContext.MissingException {

        buffer.writeInt(obj.nodeId);
        buffer.writeInt(obj.partitionId);
        buffer.writeInt(obj.partitionDepth);
        buffer.writeInt(obj.indexEntries);
        
        return buffer;
    }

    @Override
    public PacketSample decode(SerializationContext context, ByteBuf buffer) throws SerializerException, SerializationContext.MissingException {
        
        int nodeId = buffer.readInt();
        int partitionId = buffer.readInt();
        int partitionDepth = buffer.readInt();
        int indexEntries = buffer.readInt();

        return new PacketSample(partitionDepth, indexEntries, partitionId, nodeId);
    }

    @Override
    public int getSize(SerializationContext context, PacketSample obj) throws SerializerException, SerializationContext.MissingException {
        int size = 0;
        size += 4 * (Integer.SIZE/8);
        return size;
    }
}
