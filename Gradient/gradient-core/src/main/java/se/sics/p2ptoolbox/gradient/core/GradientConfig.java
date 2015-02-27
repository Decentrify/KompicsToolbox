package se.sics.p2ptoolbox.gradient.core;

/**
 * Boot up configuration for the Gradient.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientConfig {
    
    int gradientViewSize;
    int shufflePeriod;
    int seed;

    public GradientConfig(int gradientViewSize, int shufflePeriod, int seed){
        this.gradientViewSize = gradientViewSize;
        this.shufflePeriod = shufflePeriod;
        this.seed = seed;
    }


    public int getGradientViewSize() {
        return gradientViewSize;
    }

    public int getShufflePeriod() {
        return shufflePeriod;
    }

    public int getSeed() {
        return seed;
    }
}
