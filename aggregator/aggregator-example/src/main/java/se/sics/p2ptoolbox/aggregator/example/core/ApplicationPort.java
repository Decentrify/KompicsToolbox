package se.sics.p2ptoolbox.aggregator.example.core;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.PortType;
import se.sics.p2ptoolbox.aggregator.api.msg.Ready;

/**
 * Application communication port.
 *
 * Created by babbar on 2015-03-17.
 */
public class ApplicationPort extends PortType{{
    indication(Ready.class);
}}
