package se.sics.p2ptoolbox.gradient.msg;

import com.google.common.collect.ImmutableCollection;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;

/**
 * Wrapper class used by Gradient Service in order to encapsulate the Data exchanged.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class Shuffle {

    public final CroupierPeerView selfCPV;
    public final ImmutableCollection<CroupierPeerView> exchangeNodes;
    
    public Shuffle(CroupierPeerView selfCPV, ImmutableCollection<CroupierPeerView> exchangeNodes){
        this.selfCPV = selfCPV;
        this.exchangeNodes = exchangeNodes;
    }
}
