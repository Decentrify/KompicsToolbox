//package se.sics.p2ptoolbox.election.example;
//
//import se.sics.gvod.config.VodConfig;
//import se.sics.kompics.Kompics;
//import se.sics.kompics.network.netty.serialization.Serializer;
//import se.sics.kompics.network.netty.serialization.Serializers;
//import se.sics.kompics.simulation.SimulatorScheduler;
//import se.sics.p2ptoolbox.election.example.main.LEDescriptorSerializer;
//import se.sics.p2ptoolbox.election.example.main.LeaderDescriptor;
//import se.sics.p2ptoolbox.election.example.scenario.LeaderElectionScenario;
//import se.sics.p2ptoolbox.election.network.ElectionSerializerSetup;
//import se.sics.p2ptoolbox.simulator.run.LauncherComp;
//import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
//import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
//import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;
//
//import java.io.IOException;
//import java.net.InetAddress;
//
///**
// * Main Test Class for the Leader Election Protocol.
// *
// * Created by babbar on 2015-04-01.
// */
//public class MainTest {
//
//    public static long seed = 123;
//    public static String descriptorName = "leDescriptor";
//
//    public static void main(String[] args) throws IOException {
//
//        VodConfig.init(new String[]{});
//        System.out.println("Starting the serializers registration.");
//        int startId=128;
//
//        BasicSerializerSetup.registerBasicSerializers(startId);
//        startId += BasicSerializerSetup.serializerIds;
//
//        startId = ElectionSerializerSetup.registerSerializers(startId);
//        ElectionSerializerSetup.checkSetup();
//
//        Serializer leaderDescriptorSerializer = new LEDescriptorSerializer(startId);
//        Serializers.register(leaderDescriptorSerializer, descriptorName);
//        Serializers.register(LeaderDescriptor.class, descriptorName);
//
//        System.out.println("Final serializer registration complete.");
//
//        System.out.println("Setting the client address.");
//
//        LauncherComp.simulatorClientAddress = new DecoratedAddress(new BasicAddress(InetAddress.getByName("127.0.0.1"), 30000, -1));
//        LauncherComp.scheduler = new SimulatorScheduler();
//        LauncherComp.scenario = LeaderElectionScenario.boot(seed);
//
//        Kompics.setScheduler(LauncherComp.scheduler);
//        Kompics.createAndStart(LauncherComp.class, 1);
//        try {
//            Kompics.waitForTermination();
//        } catch (InterruptedException ex) {
//            throw new RuntimeException(ex.getMessage());
//        }
//
//    }
//
//
//
//}
