package se.sics.p2ptoolbox.gradient.net;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.gradient.msg.GradientShuffleNet;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;
import se.sics.p2ptoolbox.serialization.serializer.NetContentMsgSerializer;

/**
 * Serializer class used for serialization of the gradient shuffle message.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffleNetSerializer {
    
    
    public static class Request extends NetContentMsgSerializer.Request<GradientShuffleNet.Request>{


        @Override
        public GradientShuffleNet.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            return null;
        }
    }
    
    
    public static class Response extends NetContentMsgSerializer.Response<GradientShuffleNet.Response>{

        @Override
        public GradientShuffleNet.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            return null;
        }
    }
    
}
