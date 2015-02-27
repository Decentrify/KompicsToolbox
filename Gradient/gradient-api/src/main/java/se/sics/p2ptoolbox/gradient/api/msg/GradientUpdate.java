package se.sics.p2ptoolbox.gradient.api.msg;

import se.sics.gvod.common.Self;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;

import java.util.UUID;

/**
 * Gradient Update event providing the status of the gradient, in terms 
 * of gradient samples and convergence of the sample set.
 *  
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientUpdate extends GradientMsg.OneWay{
    
    public final PeerView peerView;
    
    public GradientUpdate(UUID uuid, PeerView peerView) {
        super(uuid);
        this.peerView = peerView.deepCopy();
    }
    
    public PeerView getPeerView(){
        return this.peerView;
    }
}
