package se.sics.p2ptoolbox.util.config;

import com.google.common.base.Optional;
import com.typesafe.config.Config;
import se.sics.caracaldb.Address;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * System Configuration.
 *
 * Created by babbar on 2015-08-25.
 */
public class SystemConfig {

    public final long seed;
    public final Config config;
    private final Address caracalAddress;
    public final DecoratedAddress self;
    public final Optional<DecoratedAddress> aggregator;

    public SystemConfig(long seed, Address caracalAddress, DecoratedAddress selfAddress, Optional<DecoratedAddress> aggregatorAddress, Config config){

        this.seed = seed;
        this.config = config;
        this.caracalAddress  = caracalAddress;
        this.self = selfAddress;
        this.aggregator = aggregatorAddress;
    }

    @Override
    public String toString() {
        return "SysConfig{" +
                "seed=" + seed +
                ", caracalAddress=" + caracalAddress +
                ", selfAddress=" + self +
                ", aggregatorAddress=" + aggregator +
                '}';
    }

    public long getSeed() {
        return seed;
    }

    public Address getCaracalAddress() {
        return caracalAddress;
    }

    public DecoratedAddress getSelfAddress() {
        return self;
    }

    public Optional<DecoratedAddress> getAggregatorAddress() {
        return aggregator;
    }
}
