package se.sics.p2ptoolbox.gradient.api.util;

import se.sics.gvod.common.Self;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;

import java.util.List;
import java.util.Set;

/**
 * This is gradient helper which will help the gradient with tasks of merging, filtering
 * and other important gradient operations.
 * 
 * Created by babbarshaer on 2015-02-27.
 */
public interface GradientHelper<T extends PeerView> {
    
    public void mergeGradientExchangeSample(Set<GradientPeerView> exchangeSet);

    public Set<GradientPeerView> getSampleSet();
    
    public Set<GradientPeerView> getNearestNodes(GradientPeerView gradientPeerView);
    
    public void filterEntriesBasedOnCriteria();   // TODO: Do we require this ?
    
    public void handleSelfViewUpdate(PeerView view);
    
    public boolean isConverged();

    public boolean isSampleSetEmpty();

    public GradientPeerView selectPeerToShuffleWith();
    
    public void mergeCroupierExchangeSample(List<CroupierPeerView> croupierPeerViewSet);
    
    public void incrementSampleAge();
    
}
