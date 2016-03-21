package se.sics.ktoolbox.election.api.ports;

import se.sics.ktoolbox.election.event.LeaderUpdate;
import se.sics.ktoolbox.election.event.ElectionState;
import se.sics.ktoolbox.election.event.ExtensionUpdate;
import se.sics.ktoolbox.election.event.ViewUpdate;
import se.sics.ktoolbox.election.event.LeaderState;
import se.sics.kompics.PortType;

/**
 * Main Port to communicate with the Leader Election Module.
 * Created by babbarshaer on 2015-03-27.
 */
public class LeaderElectionPort extends PortType{{
    
    request(ViewUpdate.class);
    indication(LeaderUpdate.class);

    indication(LeaderState.ElectedAsLeader.class);
    indication(LeaderState.TerminateBeingLeader.class);
    indication(ExtensionUpdate.class);

    indication(ElectionState.EnableLGMembership.class);
    indication(ElectionState.DisableLGMembership.class);

}}