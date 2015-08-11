package se.sics.p2ptoolbox.election.example.main;

import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.core.util.LeaderFilter;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Collection;

/**
 * Test Filter for the application.
 *
 * Created by babbar on 2015-04-04.
 */
public class TestFilter implements LeaderFilter{

    @Override
    public boolean initiateLeadership(Collection<DecoratedAddress> cohorts) {
        return true;
    }

    @Override
    public boolean terminateLeader(LCPeerView old, LCPeerView updated) {
        return false;
    }
}
