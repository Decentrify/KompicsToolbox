package se.sics.ktoolbox.aggregator.global.core.ports;

import se.sics.kompics.PortType;
import se.sics.ktoolbox.aggregator.global.core.event.AggregatedInfo;

/**
 * Port with which the aggregator communicates with the
 * outside application.
 *
 * Created by babbarshaer on 2015-09-02.
 */
public class GlobalAggregatorPort extends PortType{{

    indication(AggregatedInfo.class);
}}
