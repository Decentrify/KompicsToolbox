package se.sics.ktoolbox.aggregator.network.serializer;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.aggregator.network.PacketContainer;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * Setup for the serializers required to communicate with the global aggregator.
 * Created by babbar on 2015-09-09.
 */
public class AggregatorSerializerSetup {

    public static enum AggregatorSerializers {

        packetContainer(PacketContainer.class, "aggregatedStateContainer");

        public final Class serializedClass;
        public final String serializerName;

        private AggregatorSerializers(Class serializedClass, String serializerName){
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static void checkSetup() {

        for (AggregatorSerializers cs : AggregatorSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + cs.serializedClass);
            }
        }
        BasicSerializerSetup.checkSetup();
    }

    public static int registerSerializers(int startId){

        int currentId = startId;

        PacketContainerSerializer aggregatedStateContainerSerializer = new PacketContainerSerializer(currentId++);
        Serializers.register(aggregatedStateContainerSerializer, AggregatorSerializers.packetContainer.serializerName);
        Serializers.register(AggregatorSerializers.packetContainer.serializedClass, AggregatorSerializers.packetContainer.serializerName);

        return currentId;
    }

}
