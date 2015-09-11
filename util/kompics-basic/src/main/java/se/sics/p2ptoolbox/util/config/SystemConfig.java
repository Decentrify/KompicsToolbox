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
    public final DecoratedAddress self;
    public final Optional<Address> caracalAddress;
    public final Optional<DecoratedAddress> aggregator;

    public SystemConfig(Config config, long seed, DecoratedAddress selfAddress, Optional<Address> caracalAddress, 
            Optional<DecoratedAddress> aggregatorAddress){

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
                ", selfAddress=" + self +
                ", caracalAddress=" + (caracalAddress.isPresent() ? caracalAddress.get() : "x") +
                ", aggregatorAddress=" + (aggregator.isPresent() ? aggregator.get() : "x") +
                "}";
    }
}
