package se.sics.ktoolbox.aggregator.server.core;

import se.sics.kompics.Init;
import se.sics.ktoolbox.aggregator.server.api.system.DesignProcessor;

import java.util.Map;

/**
 * Init class for the visualizer component.
 *
 * Created by babbar on 2015-09-02.
 */
public class VisualizerInit extends Init<Visualizer>{

    public final int maxSnapshots;
    public final Map<String, DesignProcessor> designerNameMap;

    public VisualizerInit(int maxSnapshots, Map<String, DesignProcessor> designerNameMap){
        this.maxSnapshots = maxSnapshots;
        this.designerNameMap = designerNameMap;
    }
}
