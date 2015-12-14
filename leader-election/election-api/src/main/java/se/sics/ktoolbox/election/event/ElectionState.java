package se.sics.ktoolbox.election.event;

import se.sics.kompics.KompicsEvent;

import java.util.UUID;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * Wrapper for the marker events to let the application know about the node being elected / removed
 * to / from the leader group.
 *
 * Created by babbar on 2015-03-31.
 */
public class ElectionState{

    public static class EnableLGMembership implements ElectionEvent {
        public final Identifier id;
        public final UUID electionRoundId;
        public EnableLGMembership(Identifier id, UUID electionRoundId){
            this.id = id;
            this.electionRoundId = electionRoundId;
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }

    public static class DisableLGMembership implements KompicsEvent{
        public final Identifier id;
        public final UUID  electionRoundId;
        public DisableLGMembership(Identifier id, UUID electionRoundId){
            this.id = id;
            this.electionRoundId = electionRoundId;
        }
    }
}
