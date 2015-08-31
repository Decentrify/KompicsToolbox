package se.sics.ktoolbox.aggregator.local.example;

import se.sics.kompics.Init;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * Init for the application component.
 *
 * Created by babbar on 2015-08-31.
 */
public class ApplicationCompInit extends Init<ApplicationComp> {

    public final DecoratedAddress selfAddress;

    public ApplicationCompInit(DecoratedAddress selfAddress){
        this.selfAddress = selfAddress;
    }
}
