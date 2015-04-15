//package se.sics.p2ptoolbox.aggregator.core.net;
//
//import io.netty.buffer.ByteBuf;
//import org.javatuples.Pair;
//import se.sics.gvod.net.VodAddress;
//import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
//import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
//import se.sics.p2ptoolbox.serialization.SerializationContext;
//import se.sics.p2ptoolbox.serialization.Serializer;
//
///**
// * Serializers for the information contained in the aggregated state.
// *
// * Created by babbarshaer on 2015-03-15.
// */
//public class AggregatedStateContainerSerializer implements Serializer<AggregatedStateContainer>{
//
//    @Override
//    public ByteBuf encode(SerializationContext context, ByteBuf buf, AggregatedStateContainer obj) throws SerializerException, SerializationContext.MissingException {
//
//        Pair<Byte, Byte> pvCode = context.getCode(obj.getPacketInfo().getClass());
//        buf.writeByte(pvCode.getValue0());
//        buf.writeByte(pvCode.getValue1());
//
//        Serializer serializer = context.getSerializer(obj.getPacketInfo().getClass());
//        serializer.encode(context, buf, obj.getPacketInfo());
//        context.getSerializer(VodAddress.class).encode(context, buf, obj.getAddress());
//
//        return buf;
//
//    }
//
//    @Override
//    public AggregatedStateContainer decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
//
//        Byte pvCode0 = buf.readByte();
//        Byte pvCode1 = buf.readByte();
//        Serializer aspS = context.getSerializer(AggregatedStatePacket.class, pvCode0, pvCode1);
//        AggregatedStatePacket asp = (AggregatedStatePacket)aspS.decode(context, buf);
//        VodAddress src = context.getSerializer(VodAddress.class).decode(context, buf);
//
//        return new AggregatedStateContainer(src, asp);
//    }
//
//    @Override
//    public int getSize(SerializationContext context, AggregatedStateContainer obj) throws SerializerException, SerializationContext.MissingException {
//
//        int size = 0;
//        size += 2 * Byte.SIZE / 8; //pv code
//        Serializer pvS = context.getSerializer(obj.getPacketInfo().getClass()); // packet serializer
//        size += pvS.getSize(context, obj.getPacketInfo()); // update overall packet size.
//        size += context.getSerializer(VodAddress.class).getSize(context, obj.getAddress()); //address serializer.
//
//        return size;
//    }
//}
