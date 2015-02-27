package se.sics.p2ptoolbox.gradient.core;

import se.sics.gvod.common.Self;
import se.sics.kompics.Init;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.gradient.api.util.GradientHelper;

/**
 * Initialization Class for the Gradient Service.
 *
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientInit extends Init<Gradient> {
    
    public Self self;
    public GradientConfig gradientConfig;
    public GradientHelper<? extends PeerView> gradientHelper;
    
    public GradientInit(Self self, GradientConfig config, GradientHelper<? extends PeerView> gradientHelper){
        this.self = self;
        this.gradientConfig = config;
        this.gradientHelper = gradientHelper;
    }

    public Self getSelf() {
        return self;
    }

    public GradientConfig getGradientConfig() {
        return gradientConfig;
    }

    public GradientHelper<? extends PeerView> getGradientHelper() {
        return gradientHelper;
    }
}
