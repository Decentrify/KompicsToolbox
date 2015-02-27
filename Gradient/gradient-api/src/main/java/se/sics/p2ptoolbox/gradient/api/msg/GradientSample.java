package se.sics.p2ptoolbox.gradient.api.msg;

import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.gradient.api.util.GradientPeerView;

import java.util.Set;
import java.util.UUID;

/**
 * Set of peer views published by the gradient periodically.
 *
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientSample extends GradientMsg.OneWay {
    
    public final Set<GradientPeerView> gradientPeerViewSet;
    public final boolean isConverged;
    
    public GradientSample(UUID uuid, Set<GradientPeerView> gradientPeerViewSet, boolean isConverged) {
        super(uuid);
        this.gradientPeerViewSet = gradientPeerViewSet;
        this.isConverged = isConverged;
    }

    public boolean isConverged() {
        return isConverged;
    }

    public Set<GradientPeerView> getGradientPeerViewSet() {
        return gradientPeerViewSet;
    }
}
