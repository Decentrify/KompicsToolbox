package se.sics.p2ptoolbox.aggregator.core;

import se.sics.kompics.Init;

/**
 * Init service for the Global Aggregator Component.
 * Created by babbarshaer on 2015-03-15.
 */
public class GlobalAggregatorComponentInit extends Init<GlobalAggregatorComponent>{
    
    private long timeout;
    private long windowTimeout;
    
    public GlobalAggregatorComponentInit (long timeout, long windowTimeout){
        this.timeout = timeout;
        this.windowTimeout = windowTimeout;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getWindowTimeout() {
        return windowTimeout;
    }
}
