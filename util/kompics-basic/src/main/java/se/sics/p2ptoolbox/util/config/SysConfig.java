package se.sics.p2ptoolbox.util.config;

import com.google.common.base.Optional;
import se.sics.caracaldb.Address;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * System Configuration.
 *
 * Created by babbar on 2015-08-25.
 */
public class SysConfig {

    private final long seed;
    private final Address caracalAddress;
    private final DecoratedAddress selfAddress;
    private final Optional<DecoratedAddress> aggregatorAddress;

    public SysConfig(long seed, Address caracalAddress, DecoratedAddress selfAddress, Optional<DecoratedAddress> aggregatorAddress){

        this.seed = seed;
        this.caracalAddress  = caracalAddress;
        this.selfAddress = selfAddress;
        this.aggregatorAddress = aggregatorAddress;
    }

    @Override
    public String toString() {
        return "SysConfig{" +
                "seed=" + seed +
                ", caracalAddress=" + caracalAddress +
                ", selfAddress=" + selfAddress +
                ", aggregatorAddress=" + aggregatorAddress +
                '}';
    }

    public long getSeed() {
        return seed;
    }

    public Address getCaracalAddress() {
        return caracalAddress;
    }

    public DecoratedAddress getSelfAddress() {
        return selfAddress;
    }

    public Optional<DecoratedAddress> getAggregatorAddress() {
        return aggregatorAddress;
    }
}
