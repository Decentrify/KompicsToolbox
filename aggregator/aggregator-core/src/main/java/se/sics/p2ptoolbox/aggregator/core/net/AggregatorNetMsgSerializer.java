//package se.sics.p2ptoolbox.aggregator.core.net;
//
//import io.netty.buffer.ByteBuf;
//import org.javatuples.Pair;
//import se.sics.gvod.net.VodAddress;
//import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
//import se.sics.p2ptoolbox.aggregator.core.msg.AggregatorNetMsg;
//import se.sics.p2ptoolbox.serialization.SerializationContext;
//import se.sics.p2ptoolbox.serialization.msg.HeaderField;
//import se.sics.p2ptoolbox.serialization.serializer.NetContentMsgSerializer;
//
//import java.util.Map;
//import java.util.UUID;
//
///**
// * Message Serializer for the Direct One Way for the Aggregator Component.
// * Created by babbarshaer on 2015-03-15.
// */
//public class AggregatorNetMsgSerializer {
//
//    public static class OneWay extends NetContentMsgSerializer.OneWay<AggregatorNetMsg.OneWay> {
//
//        @Override
//        public AggregatorNetMsg.OneWay decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
//            Pair<UUID, Map<String, HeaderField>> absReq = decodeAbsOneWay(context, buf);
//            AggregatedStateContainer content = context.getSerializer(AggregatedStateContainer.class).decode(context,buf);
//            return new AggregatorNetMsg.OneWay(new VodAddress(dummyHack, -1), new VodAddress(dummyHack, -1), absReq.getValue0(), content);
//        }
//    }
//
//}
