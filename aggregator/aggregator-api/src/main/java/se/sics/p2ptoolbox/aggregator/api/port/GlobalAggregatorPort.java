package se.sics.p2ptoolbox.aggregator.api.port;

import se.sics.kompics.PortType;
import se.sics.p2ptoolbox.aggregator.api.msg.GlobalState;

/**
 * Port used to communicate with global state handler.
 * Created by babbarshaer on 2015-03-15.
 */
public class GlobalAggregatorPort extends PortType{{
    indication(GlobalState.class);
}}
