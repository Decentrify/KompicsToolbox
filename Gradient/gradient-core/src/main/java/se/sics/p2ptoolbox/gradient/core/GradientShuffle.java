package se.sics.p2ptoolbox.gradient.core;

import se.sics.p2ptoolbox.gradient.api.util.GradientPeerView;

import java.util.List;

/**
 * Wrapper class used by Gradient Service in order to encapsulate the Data exchanged.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffle {
    
    public final List<GradientPeerView> gradientExchangeNodes;
    
    public GradientShuffle(List<GradientPeerView> gradientExchangeNodes){
        this.gradientExchangeNodes = gradientExchangeNodes;
    }
    
    public List<GradientPeerView> getGradientExchangeNodes(){
        return this.gradientExchangeNodes;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GradientShuffle that = (GradientShuffle) o;

        if (gradientExchangeNodes != null ? !gradientExchangeNodes.equals(that.gradientExchangeNodes) : that.gradientExchangeNodes != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return gradientExchangeNodes != null ? gradientExchangeNodes.hashCode() : 0;
    }
}
