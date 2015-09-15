package se.sics.ktoolbox.aggregator.client.api;


import se.sics.ktoolbox.aggregator.common.PacketInfo;
import se.sics.ktoolbox.aggregator.common.Processor;

/**
 * Interface for the processing the component information and
 * generating the packet information which will be forwarded to the global aggregator.
 *
 * @param <PI_I> InputType PacketInfo
 * @param <PI_O> OutputType PacketInfo
 */
public interface ComponentInfoProcessor<PI_I extends PacketInfo, PI_O extends PacketInfo> extends Processor {


    /**
     * Each processor the component information should accept the
     * whole of the information from the component and generate a packet information object which
     * needs to be sent to the application.
     *
     * @param componentInfo Component Information
     *
     * @return Packet Information
     */
    public PI_O processComponentInfo(PI_I componentInfo);
}
