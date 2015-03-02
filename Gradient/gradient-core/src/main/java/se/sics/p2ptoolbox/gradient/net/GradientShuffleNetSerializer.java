package se.sics.p2ptoolbox.gradient.net;

import io.netty.buffer.ByteBuf;
import org.javatuples.Pair;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.gradient.core.GradientShuffle;
import se.sics.p2ptoolbox.gradient.msg.GradientShuffleNet;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;
import se.sics.p2ptoolbox.serialization.msg.HeaderField;
import se.sics.p2ptoolbox.serialization.serializer.NetContentMsgSerializer;

import java.util.Map;
import java.util.UUID;

/**
 * Serializer class used for serialization of the gradient shuffle message.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffleNetSerializer {
    
    
    public static class Request extends NetContentMsgSerializer.Request<GradientShuffleNet.Request>{


        @Override
        public GradientShuffleNet.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsRequest(context, buf);
            GradientShuffle content = context.getSerializer(GradientShuffle.class).decode(context, buf);
            return new GradientShuffleNet.Request(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);

        }
    }
    
    
    public static class Response extends NetContentMsgSerializer.Response<GradientShuffleNet.Response>{

        @Override
        public GradientShuffleNet.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsResponse(context, buf);
            GradientShuffle content = context.getSerializer(GradientShuffle.class).decode(context, buf);
            return new GradientShuffleNet.Response(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);
        }
    }
    
}
