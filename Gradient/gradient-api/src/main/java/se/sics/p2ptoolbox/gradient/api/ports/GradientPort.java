package se.sics.p2ptoolbox.gradient.api.ports;

import se.sics.kompics.PortType;
import se.sics.p2ptoolbox.gradient.api.msg.GradientSample;
import se.sics.p2ptoolbox.gradient.api.msg.GradientUpdate;

/**
 * Normal Functioning port of the gradient service.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientPort extends PortType{{
    
    request(GradientUpdate.class);
    indication(GradientSample.class);
    
}}
