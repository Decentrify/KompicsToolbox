package se.sics.p2ptoolbox.election.example;

import se.sics.gvod.config.VodConfig;
import se.sics.kompics.Kompics;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.p2ptoolbox.election.example.scenario.LeaderElectionScenario;
import se.sics.p2ptoolbox.election.network.ElectionSerializerSetup;
import se.sics.p2ptoolbox.simulator.run.LauncherComp;

import java.io.IOException;

/**
 * Main Test Class for the Leader Election Protocol.
 *
 * Created by babbar on 2015-04-01.
 */
public class MainTest {

    public static long seed = 123;

    public static void main(String[] args) throws IOException {

        VodConfig.init(new String[]{});
        System.out.println(" Serialization SetUp Successful ");

        ElectionSerializerSetup.registerSerializers(0);

        LauncherComp.scheduler = new SimulatorScheduler();
        LauncherComp.scenario = LeaderElectionScenario.boot(seed);

        Kompics.setScheduler(LauncherComp.scheduler);
        Kompics.createAndStart(LauncherComp.class, 1);
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }

    }



}
