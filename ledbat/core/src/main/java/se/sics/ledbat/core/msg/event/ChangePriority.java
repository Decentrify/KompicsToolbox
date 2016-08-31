package se.sics.ledbat.core.msg.event;


import java.util.UUID;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

public class ChangePriority implements LedbatEvent {
    public final Identifier eventId;

    UUID connectionId;
    PriorityLevel priority;

    public ChangePriority(UUID connectionId, PriorityLevel priority) {
        this.eventId = UUIDIdentifier.randomId();
        this.connectionId = connectionId;
        this.priority = priority;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }
    
    @Override
    public Identifier getId() {
        return eventId;
    }
    
    public static enum PriorityLevel {
        low(10), medium_low(50), medium_high(75), high(100);

        private double target;

        private PriorityLevel(int t) {
            target = t;
        }

        public double getTarget() {
            return target;
        }
    }
}
