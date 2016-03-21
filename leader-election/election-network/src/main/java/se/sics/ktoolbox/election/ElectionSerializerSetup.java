package se.sics.ktoolbox.election;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.election.msg.ExtensionSerializer;
import se.sics.ktoolbox.election.msg.LeaseCommitSerializer;
import se.sics.ktoolbox.election.msg.PromiseSerializer;
import se.sics.ktoolbox.election.util.PublicKeySerializer;

import java.security.PublicKey;
import se.sics.ktoolbox.election.msg.ExtensionRequest;
import se.sics.ktoolbox.election.msg.LeaseCommitUpdated;
import se.sics.ktoolbox.election.msg.Promise;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * Main class for setting up of the serializer for the election module.
 *
 * Created by babbar on 2015-04-15.
 */
public class ElectionSerializerSetup {


    /**
     * Private enum showing the list of objects that needs to be serialized as part of the leader election
     * mechanism.
     */
    private static enum ElectionSerializerEnum{
        
        promiseRequest(Promise.Request.class, "electionPromiseRequest"),
        promiseResponse(Promise.Response.class, "electionPromiseResponse"),
        leaseCommitRequest(LeaseCommitUpdated.Request.class, "electionLeaseCommitRequest"),
        leaseCommitResponse(LeaseCommitUpdated.Response.class, "electionLeaseCommitResponse"),
        extensionRequest(ExtensionRequest.class, "electionExtension"),
        publicKey(PublicKey.class, "publicKey");

        private Class serializedClass;
        private String serializerName;
        
        private ElectionSerializerEnum(Class serializedClass, String serializerName){
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
        
    }


    /**
     * Simply check that the serializers that are required by the application
     * have been registered in the system. <br/>
     * <b>CAUTION:</b> Although it might look tempting to call setups on another module but this will
     * break the independence of specific modules, so should mainly check registration of common serializers used across
     * the modules like {@link se.sics.p2ptoolbox.util.network.impl.DecoratedAddress}, {@link java.util.UUID}.
     */
    public static void checkSetup(){

        for (ElectionSerializerEnum cs : ElectionSerializerEnum.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + cs.serializedClass);
            }
        }
        BasicSerializerSetup.checkSetup();
    }


    /**
     * Register the serializers that will be used as part of the Election Protocol.
     *
     * @param startId Id to start registering
     * @return First free id after registeration.
     */
    public static int registerSerializers(int startId){

        int currentId = startId;

        PromiseSerializer.Request promiseRequestSerializer = new PromiseSerializer.Request(currentId++);
        Serializers.register(promiseRequestSerializer, ElectionSerializerEnum.promiseRequest.serializerName);
        Serializers.register(ElectionSerializerEnum.promiseRequest.serializedClass, ElectionSerializerEnum.promiseRequest.serializerName);

        PromiseSerializer.Response promiseResponseSerializer = new PromiseSerializer.Response(currentId++);
        Serializers.register(promiseResponseSerializer, ElectionSerializerEnum.promiseResponse.serializerName);
        Serializers.register(ElectionSerializerEnum.promiseResponse.serializedClass, ElectionSerializerEnum.promiseResponse.serializerName);

        LeaseCommitSerializer.Request leaseCommitRequestSerializer = new LeaseCommitSerializer.Request(currentId++);
        Serializers.register(leaseCommitRequestSerializer, ElectionSerializerEnum.leaseCommitRequest.serializerName);
        Serializers.register(ElectionSerializerEnum.leaseCommitRequest.serializedClass, ElectionSerializerEnum.leaseCommitRequest.serializerName);

        LeaseCommitSerializer.Response leaseCommitResponseSerializer = new LeaseCommitSerializer.Response(currentId++);
        Serializers.register(leaseCommitResponseSerializer, ElectionSerializerEnum.leaseCommitResponse.serializerName);
        Serializers.register(ElectionSerializerEnum.leaseCommitResponse.serializedClass, ElectionSerializerEnum.leaseCommitResponse.serializerName);

        ExtensionSerializer extensionSerializer = new ExtensionSerializer(currentId++);
        Serializers.register(extensionSerializer, ElectionSerializerEnum.extensionRequest.serializerName);
        Serializers.register(ElectionSerializerEnum.extensionRequest.serializedClass, ElectionSerializerEnum.extensionRequest.serializerName);

        PublicKeySerializer publicKeySerializer = new PublicKeySerializer(currentId++);
        Serializers.register(publicKeySerializer, ElectionSerializerEnum.publicKey.serializerName);
        Serializers.register(ElectionSerializerEnum.publicKey.serializedClass, ElectionSerializerEnum.publicKey.serializerName);

        return currentId;
    }
}
