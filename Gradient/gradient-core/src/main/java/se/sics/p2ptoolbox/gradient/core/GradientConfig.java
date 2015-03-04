package se.sics.p2ptoolbox.gradient.core;

/**
 * Boot up configuration for the Gradient.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientConfig {
    
    public final int viewSize;
    public final long shufflePeriod;
    public final int shuffleLength;

    public GradientConfig(int viewSize, long shufflePeriod, int shuffleLength){
        this.viewSize = viewSize;
        this.shufflePeriod = shufflePeriod;
        this.shuffleLength = shuffleLength;
    }
}
