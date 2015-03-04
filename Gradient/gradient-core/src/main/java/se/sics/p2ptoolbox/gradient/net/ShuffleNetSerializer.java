package se.sics.p2ptoolbox.gradient.net;

import io.netty.buffer.ByteBuf;
import org.javatuples.Pair;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.gradient.msg.Shuffle;
import se.sics.p2ptoolbox.gradient.msg.ShuffleNet;
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
public class ShuffleNetSerializer {
    
    
    public static class Request extends NetContentMsgSerializer.Request<ShuffleNet.Request>{


        @Override
        public ShuffleNet.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsRequest(context, buf);
            Shuffle content = context.getSerializer(Shuffle.class).decode(context, buf);
            return new ShuffleNet.Request(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);

        }
    }
    
    
    public static class Response extends NetContentMsgSerializer.Response<ShuffleNet.Response>{

        @Override
        public ShuffleNet.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsResponse(context, buf);
            Shuffle content = context.getSerializer(Shuffle.class).decode(context, buf);
            return new ShuffleNet.Response(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), absReq.getValue1(), content);
        }
    }
    
}
