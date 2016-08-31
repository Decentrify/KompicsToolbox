package se.sics.ledbat.core.util;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**

 */
public class ThroughputHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ThroughputHandler.class);
    private long currentSecondNumOfBytes;
    private String connectionId;

    public static List<Long> throughPutHistory = new ArrayList<Long>(400);
    private long currentSecond;
    private static final int TIME_STEP = 1 ;


    public ThroughputHandler(String connectionId) {
        this.connectionId = connectionId;
    }

    public void packetReceived(int size) {
        long now = System.currentTimeMillis()/1000;
        if (now > currentSecond + (TIME_STEP-1)) {
            LOG.info(connectionId + "\t"+  currentSecondNumOfBytes/TIME_STEP);
            currentSecond = now ;
            currentSecondNumOfBytes = size;
        } else {
            currentSecondNumOfBytes += size;
        }
    }

    public void packetSend(int size) {
        long now = System.currentTimeMillis()/1000;
        if (now > currentSecond) {
            throughPutHistory.add(currentSecondNumOfBytes);
            currentSecond = now ;
            currentSecondNumOfBytes = size;
        } else {
            currentSecondNumOfBytes += size;
        }
    }
}
