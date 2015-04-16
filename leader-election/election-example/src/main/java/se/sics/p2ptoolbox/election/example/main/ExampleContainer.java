package se.sics.p2ptoolbox.election.example.main;

import se.sics.p2ptoolbox.util.Container;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * Container for the test application.
 * Created by babbar on 2015-04-16.
 */
public class ExampleContainer implements Container<DecoratedAddress, LeaderDescriptor> {

    private DecoratedAddress source;
    private LeaderDescriptor leaderDescriptor;

    public ExampleContainer(DecoratedAddress source, LeaderDescriptor leaderDescriptor){
        this.source = source;
        this.leaderDescriptor = leaderDescriptor;
    }

    @Override
    public DecoratedAddress getSource() {
        return this.source;
    }

    @Override
    public LeaderDescriptor getContent() {
        return this.leaderDescriptor;
    }
}
