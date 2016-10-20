package se.sics.ledbat.core.util;

import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ThroughputHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ThroughputHandler.class);

    private static final int TIME_STEP = 1;
    private static final int HISTORY_SIZE = 60; //1min

    private final String connectionId;
    private final LinkedList<Long> throughPutHistory = new LinkedList<>();
    private long currentSecond;
    private long currentSecondNumOfBytes;

    public ThroughputHandler(String connectionId) {
        this.connectionId = connectionId;
        this.throughPutHistory.add(0l);
    }

    private void update(long now) {
        long nowSeconds = now / 1000;
        if (nowSeconds > currentSecond + (TIME_STEP - 1)) {
            LOG.info(connectionId + "\t" + currentSecondNumOfBytes / TIME_STEP);
            throughPutHistory.add(currentSecondNumOfBytes);
            if (throughPutHistory.size() > HISTORY_SIZE) {
                throughPutHistory.removeFirst();
            }
            currentSecond = nowSeconds;
            currentSecondNumOfBytes = 0;
        }
    }

    public void packetReceived(long now, int size) {
        update(now);
        currentSecondNumOfBytes += size;
    }

    public long speed(long now) {
        update(now);
        return throughPutHistory.getLast();
    }
    
    public long currentSpeed(long now) {
        update(now);
        return currentSecondNumOfBytes;
    }
}