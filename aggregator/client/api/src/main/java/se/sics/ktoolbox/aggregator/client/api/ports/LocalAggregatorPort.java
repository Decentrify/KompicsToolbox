package se.sics.ktoolbox.aggregator.client.api.ports;


import se.sics.kompics.PortType;
import se.sics.ktoolbox.aggregator.client.api.events.ComponentInfoEvent;
import se.sics.ktoolbox.aggregator.client.api.events.UpdateEvent;

/**
 * Main Port for interaction with the local aggregator.
 *
 * Created by babbar on 2015-08-31.
 */
public class LocalAggregatorPort extends PortType {{

    request(ComponentInfoEvent.class);
    request(UpdateEvent.class);
}}
