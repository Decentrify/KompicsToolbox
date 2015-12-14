package se.sics.ktoolbox.election.util;

import se.sics.ktoolbox.util.network.KAddress;

/**
 * Container for the Leader Election Capable View.
 *
 * Created by babbar on 2015-03-31.
 */
public class LEContainer {

    KAddress source;
    LCPeerView lcp;


    public LEContainer(KAddress source, LCPeerView lcp){
        this.source = source;
        this.lcp = lcp;
    }

    public KAddress getSource(){
        return this.source;
    }

    public LCPeerView getLCPeerView(){
        return this.lcp;
    }

    @Override
    public String toString() {
        return "LEContainer{" +
                "source=" + source +
                ", lcp=" + lcp +
                '}';
    }
}
