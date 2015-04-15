package se.sics.p2ptoolbox.election.network;

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

    }


    /**
     * Simply check that the serializers that are required by the application
     * have been registered in the system. <br/>
     * <b>CAUTION:</b> Although it might look tempting to call setups on another module but this will
     * break the independence of specific modules, so should mainly check registration of common serializers used across
     * the modules like {@link se.sics.p2ptoolbox.util.network.impl.DecoratedAddress}, {@link java.util.UUID}.
     */
    public static void checkSetup(){

    }


    /**
     * Register the serializers that will be used as part of the Election Protocol.
     *
     * @param startId Id to start registering
     * @return First free id after registeration.
     */
    public static int registerSerializers(int startId){

        int currentId = startId;

        return currentId;
    }





}
