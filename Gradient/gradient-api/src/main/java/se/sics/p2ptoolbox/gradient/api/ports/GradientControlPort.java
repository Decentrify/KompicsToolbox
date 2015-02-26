package se.sics.p2ptoolbox.gradient.api.ports;

import se.sics.kompics.PortType;
import se.sics.p2ptoolbox.gradient.api.msg.GradientDisconnected;
import se.sics.p2ptoolbox.gradient.api.msg.GradientJoin;

/**
 * Port on which gradient interacts with the control messages
 * as part of gradient service.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientControlPort extends PortType{{
    
    request(GradientJoin.class);
    indication(GradientDisconnected.class);
    
}}
