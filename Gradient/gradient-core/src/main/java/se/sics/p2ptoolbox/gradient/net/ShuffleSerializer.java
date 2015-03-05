package se.sics.p2ptoolbox.gradient.net;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.gradient.msg.Shuffle;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;

/**
 * Created by babbarshaer on 2015-03-01.
 */
public class ShuffleSerializer implements Serializer<Shuffle> {

    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, Shuffle obj) throws SerializerException, SerializationContext.MissingException {
        Serializer<CroupierPeerView> cpvSerializer = context.getSerializer(CroupierPeerView.class);

        cpvSerializer.encode(context, buf, obj.selfCPV);
        buf.writeInt(obj.exchangeNodes.size());
        for (CroupierPeerView cpv : obj.exchangeNodes) {
            cpvSerializer.encode(context, buf, cpv);
        }

        return buf;
    }

    @Override
    public Shuffle decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
        Serializer<CroupierPeerView> cpvSerializer = context.getSerializer(CroupierPeerView.class);

        CroupierPeerView selfCPV = cpvSerializer.decode(context, buf);
        int exchangeNodesSize = buf.readInt();
        ImmutableList.Builder<CroupierPeerView> exchangeNodes = new ImmutableList.Builder<CroupierPeerView>();
        for (int i = 0; i < exchangeNodesSize; i++) {
            exchangeNodes.add(cpvSerializer.decode(context, buf));
        }

        return new Shuffle(selfCPV, exchangeNodes.build());
    }

    @Override
    public int getSize(SerializationContext context, Shuffle obj) throws SerializerException, SerializationContext.MissingException {
        Serializer<CroupierPeerView> cpvSerializer = context.getSerializer(CroupierPeerView.class);

        int size = 0;
        size += cpvSerializer.getSize(context, obj.selfCPV);
        size += Integer.SIZE / 8; //exchangeNodes collection size
        for (CroupierPeerView cpv : obj.exchangeNodes) {
            size += cpvSerializer.getSize(context, cpv);
        }

        return size;
    }
}
