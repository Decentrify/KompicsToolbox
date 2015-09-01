package se.sics.ktoolbox.aggregator.local.api;

import se.sics.ktoolbox.aggregator.global.api.PacketInfo;

/**
 * Interface for the processing the component information and
 * generating the packet information which will be forwarded to the global aggregator.
 *
 * Created by babbar on 2015-08-31.
 */
public interface ComponentInfoProcessor {


    /**
     * Each processor the component information should accept the
     * whole of the information from the component and generate a packet information object which
     * needs to be sent to the application.
     *
     * @param componentInfo Component Information
     *
     * @return Packet Information
     */
    public PacketInfo processComponentInfo(ComponentInfo componentInfo);
}
