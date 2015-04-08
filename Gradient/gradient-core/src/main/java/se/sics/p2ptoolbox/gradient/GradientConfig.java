package se.sics.p2ptoolbox.gradient;

/**
 * Boot up configuration for the Gradient.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientConfig {
    
    public final int viewSize;
    public final long shufflePeriod;
    public final int shuffleLength;
    public final double softMaxTemp;

    public GradientConfig(int viewSize, long shufflePeriod, int shuffleLength, double softMaxTemp){
        this.viewSize = viewSize;
        this.shufflePeriod = shufflePeriod;
        this.shuffleLength = shuffleLength;
        this.softMaxTemp = softMaxTemp;
    }
}
