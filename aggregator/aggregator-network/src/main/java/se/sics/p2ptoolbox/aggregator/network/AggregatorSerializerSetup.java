package se.sics.p2ptoolbox.aggregator.network;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
import se.sics.p2ptoolbox.aggregator.network.util.AggregatedStateContainerSerializer;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * Main Serializer Setup class for the aggregator.
 * Created by babbar on 2015-04-15.
 */
public class AggregatorSerializerSetup {

    public static int serializerIds = 1;

    public static enum AggregatorSerializers {

        aggregatedStateContainer(AggregatedStateContainer.class, "aggregatedStateContainer");

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

        AggregatedStateContainerSerializer aggregatedStateContainerSerializer = new AggregatedStateContainerSerializer(currentId++);
        Serializers.register(aggregatedStateContainerSerializer, AggregatorSerializers.aggregatedStateContainer.serializerName);
        Serializers.register(AggregatorSerializers.aggregatedStateContainer.serializedClass, AggregatorSerializers.aggregatedStateContainer.serializerName);

        return currentId;
    }

}
