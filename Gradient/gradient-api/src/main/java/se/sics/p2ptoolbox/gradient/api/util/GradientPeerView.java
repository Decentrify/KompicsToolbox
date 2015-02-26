package se.sics.p2ptoolbox.gradient.api.util;

import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;

/**
 * Wrapper Over the Descriptor sent to the Gradient for Exchange.
 *
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientPeerView {
    
    public final PeerView peerView;
    private int age;
    public final VodAddress src;
    
    public GradientPeerView(PeerView peerView, VodAddress src){
        this.peerView = peerView;
        this.src=src;
        this.age=0;
    }
    
    public GradientPeerView(PeerView peerView, VodAddress src, int age){
        this.peerView = peerView;
        this.src = src;
        this.age = age;
    }
    
    public void incrementAge() {
        age++;
    }

    public int getAge() {
        return age;
    }
    
    @Override
    public String toString(){
        return ("<" + src +"," + age +">" + peerView);
    }
    
}
