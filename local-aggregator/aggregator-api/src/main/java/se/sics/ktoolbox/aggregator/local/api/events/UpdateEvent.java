package se.sics.ktoolbox.aggregator.local.api.events;

import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * Update Event sent by the application indicating update in the
 * address on the node.
 *
 * Created by babbar on 2015-08-31.
 */
public class UpdateEvent implements KompicsEvent{

    public final DecoratedAddress selfAddress;

    public UpdateEvent(DecoratedAddress address){
        this.selfAddress = address;
    }

}
