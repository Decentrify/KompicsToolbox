package se.sics.cm;

import se.sics.cm.utils.Config;
import se.sics.gvod.config.AbstractConfiguration;

/**
 * Created by alidar on 10/23/14.
 */
public class ChunkManagerConfiguration
        extends AbstractConfiguration<ChunkManagerConfiguration> {

    private int fragmentThreshold;
    private int receiveMessageTimeout;

    public ChunkManagerConfiguration() {
        this(Config.FRAGMENTATION_THRESHOLD, Config.RECEIVE_MESSAGE_TIMEOUT);
    }

    public ChunkManagerConfiguration(int fragmentThreshold, int receiveMessageTimeout) {
        this.fragmentThreshold = fragmentThreshold;
        this.receiveMessageTimeout = receiveMessageTimeout;
    }


    public int getFragmentThreshold() {
        return fragmentThreshold;
    }

    public void setFragmentThreshold(int fragmentThreshold) {
        this.fragmentThreshold = fragmentThreshold;
    }

    public int getReceiveMessageTimeout() {
        return receiveMessageTimeout;
    }

    public void setReceiveMessageTimeout(int receiveMessageTimeout) {
        this.receiveMessageTimeout = receiveMessageTimeout;
    }
}
