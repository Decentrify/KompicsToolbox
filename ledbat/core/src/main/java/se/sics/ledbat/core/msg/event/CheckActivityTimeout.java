package se.sics.ledbat.core.msg.event;

import java.util.UUID;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.ktoolbox.util.network.KAddress;


/**
 *
 */
public class CheckActivityTimeout extends Timeout {

    private KAddress serverAddress;
    UUID connId ;

    public CheckActivityTimeout(SchedulePeriodicTimeout request, UUID connId) {
        super(request);
        this.connId = connId;
    }

    public KAddress getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(KAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public UUID getConnId() {
        return connId;
    }

    public void setConnId(UUID connId) {
        this.connId = connId;
    }
}
