package se.sics.ktoolbox.aggregator.server.api.ports;

import se.sics.kompics.PortType;
import se.sics.ktoolbox.aggregator.server.api.event.WindowProcessing;

/**
 * Port for interaction with the visualizer component
 * in the system.
 *
 * Created by babbar on 2015-09-02.
 */
public class VisualizerPort extends PortType{{

    request(WindowProcessing.Request.class);
    indication(WindowProcessing.Response.class);

}}
