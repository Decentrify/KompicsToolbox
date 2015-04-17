package se.sics.p2ptoolbox.election.api.msg.mock;

import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.Container;

import java.util.Collection;

/**
 * Gradient Update from the Mocked Component to be used for testing purposes.
 * Created by babbar on 2015-04-01.
 */
public class MockedGradientUpdate implements KompicsEvent {

    public final Collection<Container> collection;

    public MockedGradientUpdate(Collection<Container> collection){
        this.collection = collection;
    }

}
