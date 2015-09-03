package se.sics.ktoolbox.aggregator.global.api.system;

import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.Collection;
import java.util.Map;

/**
 * Interface indicating the designer used for
 * a particular type of stats collection and visualization.
 *
 * Created by babbar on 2015-09-02.
 */
public interface Designer {


    /**
     * Process the windows being buffered by the visualization window.
     * After processing return instance of the  processed windows.
     *
     * @param windows : windows are the snapshots of the systems at different intervals.
     *
     * @return collection of processed windows.
     */
    public Collection<ProcessedWindow> process(Collection<Map<BasicAddress, Collection<PacketInfo>>> windows);


}
