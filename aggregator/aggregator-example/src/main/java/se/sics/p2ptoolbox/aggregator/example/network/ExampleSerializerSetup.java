package se.sics.p2ptoolbox.aggregator.example.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.aggregator.example.core.PacketSample;
import se.sics.p2ptoolbox.aggregator.network.AggregatorSerializerSetup;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * Serializer Setup done by the application.
 *
 * Created by babbar on 2015-04-15.
 */
public class ExampleSerializerSetup {


    public static Logger logger = LoggerFactory.getLogger(ExampleSerializerSetup.class);

    public static enum ExampleSerializers {

        packetSample(PacketSample.class, "packetSample");

        public final Class serializedClass;
        public final String serializerName;

        private ExampleSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static void oneTimeSetup(){

        logger.info("One time setup called for the Aggregator Application");

        int currentId = 0;

        BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId += BasicSerializerSetup.serializerIds;

        currentId = AggregatorSerializerSetup.registerSerializers(currentId);
        registerSerializer(currentId);
    }

    private static int registerSerializer(int startId){

        int currentId = startId;
        PacketSampleSerializer sampleSerializer = new PacketSampleSerializer(currentId++);
        Serializers.register(sampleSerializer, ExampleSerializers.packetSample.serializerName);
        Serializers.register(ExampleSerializers.packetSample.serializedClass, ExampleSerializers.packetSample.serializerName);

        return currentId;
    }

}
