package se.sics.ledbat.core.util;

import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**

 */
public class ThroughputHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ThroughputHandler.class);
    
    private static final int TIME_STEP = 1 ;
    private static final int HISTORY_SIZE = 600; //10mins

    private final String connectionId;
    private final LinkedList<Long> throughPutHistory = new LinkedList<>();
    private long currentSecond;
    private long currentSecondNumOfBytes;
    

    public ThroughputHandler(String connectionId) {
        this.connectionId = connectionId;
        this.throughPutHistory.add(0l);
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
            if(throughPutHistory.size() > HISTORY_SIZE) {
                throughPutHistory.removeFirst();
            }
            currentSecond = now ;
            currentSecondNumOfBytes = size;
        } else {
            currentSecondNumOfBytes += size;
        }
    }
    
    public long speed() {
        return throughPutHistory.getLast();
    }
}
