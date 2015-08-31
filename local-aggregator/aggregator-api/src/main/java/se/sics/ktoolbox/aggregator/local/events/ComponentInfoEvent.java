package se.sics.ktoolbox.aggregator.local.events;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.aggregator.local.ComponentInfo;

/**
 * Event carrying the component information which will be
 * locally aggregated by the local aggregator.
 *
 * Created by babbar on 2015-08-31.
 */
public class ComponentInfoEvent implements KompicsEvent {


    public final ComponentInfo componentInfo;
    public final Integer overlayId;

    public ComponentInfoEvent(Integer overlayId, ComponentInfo componentInfo) {

        this.overlayId = overlayId;
        this.componentInfo = componentInfo;
    }


}
