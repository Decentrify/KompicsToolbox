//package se.sics.p2ptoolbox.aggregator.core;
//
//import se.sics.gvod.common.msgs.DirectMsgNetty;
//import se.sics.gvod.net.VodAddress;
//import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
//import se.sics.p2ptoolbox.aggregator.core.msg.AggregatorNetMsg;
//import se.sics.p2ptoolbox.aggregator.core.net.AggregatedStateContainerSerializer;
//import se.sics.p2ptoolbox.aggregator.core.net.AggregatorNetMsgSerializer;
//import se.sics.p2ptoolbox.serialization.SerializationContext;
//import se.sics.p2ptoolbox.serialization.msg.NetMsg;
//import se.sics.p2ptoolbox.serialization.msg.OverlayHeaderField;
//import se.sics.p2ptoolbox.serialization.serializer.SerializerAdapter;
//
//import java.util.UUID;
//
///**
// * Place holder for the aggregator network settings.
// * Created by babbarshaer on 2015-03-16.
// */
//public class AggregatorNetworkSettings {
//
//
//    private static SerializationContext context = null;
//    private static final String NET_ONE_WAY = "AGGREGATOR_ONE_WAY";
//
//    public static void oneTimeSetup(SerializationContext setContext, byte aggregatorOneWayAlias) {
//        if(context != null) {
//            throw new RuntimeException("aggregator has already been setup - do not call this multiple times(for each aggregator instance)");
//        }
//        context = setContext;
//
//        registerNetworkMsg(aggregatorOneWayAlias);
//        registerOthers();
//
//        checkSetup();
//    }
//
//    private static void registerNetworkMsg(byte aggregatorOneWayAlias) {
//        try {
//
//            context.registerAlias(DirectMsgNetty.Oneway.class, NET_ONE_WAY, aggregatorOneWayAlias);
//            context.registerSerializer(AggregatorNetMsg.OneWay.class, new AggregatorNetMsgSerializer.OneWay());
//            context.multiplexAlias(NET_ONE_WAY, AggregatorNetMsg.OneWay.class, (byte)0x01);
//
//        } catch (SerializationContext.DuplicateException ex) {
//            throw new RuntimeException(ex);
//        } catch (SerializationContext.MissingException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    private static void registerOthers() {
//        try {
//            context.registerSerializer(AggregatedStateContainer.class, new AggregatedStateContainerSerializer());
//        } catch (SerializationContext.DuplicateException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    private static void checkSetup() {
//        if (context == null || !NetMsg.hasContext() || !SerializerAdapter.hasContext()) {
//            throw new RuntimeException("serialization context not set");
//        }
//
//        try {
//            for (OtherSerializers serializedClass : OtherSerializers.values()) {
//                context.getSerializer(serializedClass.serializedClass);
//            }
//        } catch (SerializationContext.MissingException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
////    public static enum MsgAliases {
////
////        CROUPIER_NET_REQUEST(DirectMsgNetty.Request.class), CROUPIER_NET_RESPONSE(DirectMsgNetty.Response.class);
////        public final Class aliasedClass;
////
////        MsgAliases(Class aliasedClass) {
////            this.aliasedClass = aliasedClass;
////        }
////    }
//
//    public static enum OtherSerializers {
//        UUID(java.util.UUID.class), VOD_ADDRESS(VodAddress.class);
//
//        public final Class serializedClass;
//
//        OtherSerializers(Class serializedClass) {
//            this.serializedClass = serializedClass;
//        }
//    }
//
//
//
//}
