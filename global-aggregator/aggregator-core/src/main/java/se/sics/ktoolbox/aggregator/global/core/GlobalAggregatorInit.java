package se.sics.ktoolbox.aggregator.global.core;

import se.sics.kompics.Init;

/**
 * Main initialization class for the aggregator component.
 *
 * Created by babbarshaer on 2015-09-01.
 */
public class GlobalAggregatorInit extends Init<GlobalAggregator> {

    public final long timeout;

    public GlobalAggregatorInit(long timeout){
        this.timeout = timeout;
    }

}
