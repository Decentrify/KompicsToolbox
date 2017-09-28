package se.sics.ktoolbox.election.event;

import java.util.UUID;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;

/**
 * Wrapper for the marker events to let the application know about the node being elected / removed
 * to / from the leader group.
 *
 * Created by babbar on 2015-03-31.
 */
public class ElectionState{

    public static class EnableLGMembership implements ElectionEvent {
        public final Identifier eventId;
        public final UUID electionRoundId;
        public EnableLGMembership(Identifier eventId, UUID electionRoundId){
            this.eventId = eventId;
            this.electionRoundId = electionRoundId;
        }
        
        public EnableLGMembership(UUID electionRoundId){
            this(BasicIdentifiers.eventId(), electionRoundId);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
    }

    public static class DisableLGMembership implements KompicsEvent{
        public final Identifier eventId;
        public final UUID  electionRoundId;
        public DisableLGMembership(Identifier eventId, UUID electionRoundId){
            this.eventId = eventId;
            this.electionRoundId = electionRoundId;
        }
        
        public DisableLGMembership(UUID electionRoundId){
            this(BasicIdentifiers.eventId(), electionRoundId);
        }
    }
}
