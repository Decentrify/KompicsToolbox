package se.sics.ktoolbox.aggregator.global.api.system;

import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.Collection;
import java.util.Map;

/**
 * Design Processor indicates the use of the processor for creating
 * visualizations in the system.
 *
 * @param <PI_I> Packet Information. ( Only to enforce compile time check. )
 * @param <DI_O> Design Information.
 */
public interface DesignProcessor < PI_I extends PacketInfo, DI_O extends DesignInfo > extends Processor{


    /**
     * Process the windows being buffered by the visualization window.
     * After processing return instance of the  processed windows.
     *
     * @param windows : windows are the snapshots of the systems at different intervals.
     *
     * @return collection of processed windows.
     */
    public DesignInfoContainer<DI_O> process(Collection<Map<BasicAddress, Collection<PacketInfo>>> windows);


    /**
     * An explicit command indicating the designer to clean the
     * internal state which might still be held in regard to the
     * previous processing.
     *
     */
    public void cleanState();

}
