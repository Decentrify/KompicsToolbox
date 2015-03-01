package se.sics.p2ptoolbox.gradient.net;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.gradient.core.GradientShuffle;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * Serializer for the Gradient Shuffle Wrapper.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffleSerializer implements Serializer<GradientShuffle> {
    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, GradientShuffle obj) throws SerializerException, SerializationContext.MissingException {
        return null;
    }

    @Override
    public GradientShuffle decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
        return null;
    }

    @Override
    public int getSize(SerializationContext context, GradientShuffle obj) throws SerializerException, SerializationContext.MissingException {
        return 0;
    }
}
