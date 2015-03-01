package se.sics.p2ptoolbox.gradient.net;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.gradient.api.util.GradientPeerView;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * Serializer for the descriptor wrapper.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientPVSerializer implements Serializer<GradientPeerView> {
    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, GradientPeerView obj) throws SerializerException, SerializationContext.MissingException {
        return null;
    }

    @Override
    public GradientPeerView decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
        return null;
    }

    @Override
    public int getSize(SerializationContext context, GradientPeerView obj) throws SerializerException, SerializationContext.MissingException {
        return 0;
    }
}
