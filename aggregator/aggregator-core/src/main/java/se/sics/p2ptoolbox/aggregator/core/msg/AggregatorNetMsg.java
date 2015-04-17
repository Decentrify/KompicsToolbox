//package se.sics.p2ptoolbox.aggregator.core.msg;
//
//import se.sics.gvod.net.VodAddress;
//import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
//import se.sics.p2ptoolbox.serialization.msg.NetContentMsg;
//
//import java.util.UUID;
//
///**
// * Direct Message for the Global Aggregator Component.
// *
// * Created by babbarshaer on 2015-03-15.
// */
//public class AggregatorNetMsg {
//
//    public static class OneWay extends NetContentMsg.OneWay<AggregatedStateContainer>{
//
//        public OneWay(VodAddress src, VodAddress dest, UUID id, AggregatedStateContainer content) {
//            super(src, dest, id, content);
//        }
//
//        @Override
//        public OneWay copy() {
//            return new OneWay(vodSrc, vodDest, id, content);
//        }
//
//        public String toString(){
//            return "AGGREGATOR_ONE_WAY" + " source: " + vodSrc.getPeerAddress() + " destination: " + vodDest.getPeerAddress();
//        }
//
//    }
//
//}
