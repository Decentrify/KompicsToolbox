package se.sics.ktoolbox.aggregator.global.example.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Kompics;

/**
 * Main bootup class.
 *
 * Created by babbarshaer on 2015-09-06.
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        logger.debug("Initiating the main launch for the local aggregator example.");

        if (Kompics.isOn()) {
            logger.debug("Shutting down the previous kompics instance.");
            Kompics.shutdown();
        }
        Kompics.createAndStart(Launcher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
    }



}