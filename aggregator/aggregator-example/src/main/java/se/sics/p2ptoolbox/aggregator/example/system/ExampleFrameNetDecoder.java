//package se.sics.p2ptoolbox.aggregator.example.system;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import se.sics.gvod.common.msgs.MessageDecodingException;
//import se.sics.gvod.net.BaseMsgFrameDecoder;
//import se.sics.gvod.net.VodAddress;
//import se.sics.gvod.net.msgs.RewriteableMsg;
//import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
//import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
//import se.sics.p2ptoolbox.aggregator.core.AggregatorNetworkSettings;
//import se.sics.p2ptoolbox.aggregator.example.core.PacketSample;
//import se.sics.p2ptoolbox.aggregator.example.core.PacketSampleSerializer;
//import se.sics.p2ptoolbox.aggregator.example.core.Peer;
//import se.sics.p2ptoolbox.serialization.SerializationContext;
//import se.sics.p2ptoolbox.serialization.SerializationContextImpl;
//import se.sics.p2ptoolbox.serialization.msg.HeaderField;
//import se.sics.p2ptoolbox.serialization.msg.NetMsg;
//import se.sics.p2ptoolbox.serialization.serializer.SerializerAdapter;
//import se.sics.p2ptoolbox.serialization.serializer.UUIDSerializer;
//import se.sics.p2ptoolbox.serialization.serializer.VodAddressSerializer;
//
//import java.util.UUID;
//
///**
//* Created by babbarshaer on 2015-03-15.
//*/
//public class ExampleFrameNetDecoder extends BaseMsgFrameDecoder {
//
//
//    public static final byte AGGREGATOR_ONE_WAY = (byte)0x90;
//    //other aliases
//    public static final byte HEADER_FIELD_CODE = (byte) 0x01;
//    public static final byte AGGREGATED_STATE_PACKET_CODE = (byte) 0x02;
//
//    public static final String HEADER_FIELD_ALIAS = "MY_EXAMPLE_HEADER_FIELD";
//    public static final String AGGREGATED_STATE_PACKET_ALIAS = "MY_STATE_PACKET";
//
//    private static final SerializationContext context = new SerializationContextImpl();
//
//    public static void init() {
//
//        NetMsg.setContext(context);
//        SerializerAdapter.setContext(context);
//
//        try {
//            context.registerAlias(HeaderField.class, HEADER_FIELD_ALIAS, HEADER_FIELD_CODE);
//            context.registerSerializer(UUID.class, new UUIDSerializer());
//            context.registerSerializer(VodAddress.class, new VodAddressSerializer());
//
//            context.registerAlias(AggregatedStatePacket.class, AGGREGATED_STATE_PACKET_ALIAS, AGGREGATED_STATE_PACKET_CODE);
//            context.registerSerializer(PacketSample.class, new PacketSampleSerializer());
//            context.multiplexAlias(AGGREGATED_STATE_PACKET_ALIAS, PacketSample.class, (byte) 0x01);
//
//        } catch (SerializationContext.DuplicateException ex) {
//            throw new RuntimeException(ex);
//        }
//        catch (SerializationContext.MissingException ex) {
//            throw new RuntimeException(ex);
//        }
//
//        AggregatorNetworkSettings.oneTimeSetup(context, AGGREGATOR_ONE_WAY);
//    }
//
//    public ExampleFrameNetDecoder() {
//        super();
//    }
//
//    @Override
//    protected RewriteableMsg decodeMsg(ChannelHandlerContext ctx, ByteBuf buffer) throws MessageDecodingException {
//        // See if msg is part of parent project, if yes then return it.
//        // Otherwise decode the msg here.
//        RewriteableMsg msg = super.decodeMsg(ctx, buffer);
//        if (msg != null) {
//            return msg;
//        }
//
//        switch (opKod) {
//            case AGGREGATOR_ONE_WAY:
//                SerializerAdapter.OneWay oneWay = new SerializerAdapter.OneWay();
//                return oneWay.decodeMsg(buffer);
//            default:
//                return null;
//        }
//    }
//}
