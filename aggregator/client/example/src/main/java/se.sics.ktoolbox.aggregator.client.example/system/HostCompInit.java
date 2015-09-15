package se.sics.ktoolbox.aggregator.client.example.system;

import se.sics.kompics.Init;
import se.sics.p2ptoolbox.util.config.SystemConfig;

/**
 * Initializer for the Host Component.
 *
 * Created by babbar on 2015-08-31.
 */
public class HostCompInit extends Init<HostComp> {

    public final SystemConfig systemConfig;

    public HostCompInit(SystemConfig systemConfig){
        this.systemConfig = systemConfig;
    }

}
