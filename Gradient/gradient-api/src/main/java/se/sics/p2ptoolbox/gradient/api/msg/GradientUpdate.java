package se.sics.p2ptoolbox.gradient.api.msg;

import se.sics.p2ptoolbox.croupier.api.util.PeerView;

import java.util.UUID;

/**
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientUpdate extends GradientMsg.OneWay{
    
    public final PeerView peerview;
    
    public GradientUpdate(UUID uuid, PeerView peerView) {
        super(uuid);
        this.peerview = peerView;
    }
    
    public PeerView getPeerView(){
        return this.peerview;
    }
}
