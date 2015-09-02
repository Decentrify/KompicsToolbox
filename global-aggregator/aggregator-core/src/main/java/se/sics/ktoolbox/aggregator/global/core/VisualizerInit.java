package se.sics.ktoolbox.aggregator.global.core;

import se.sics.kompics.Init;

/**
 * Init class for the visualizer component.
 *
 * Created by babbar on 2015-09-02.
 */
public class VisualizerInit extends Init<Visualizer>{

    public final int maxSnapshots;

    public VisualizerInit(int maxSnapshots){
        this.maxSnapshots = maxSnapshots;
    }
}
