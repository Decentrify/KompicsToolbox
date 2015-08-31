package se.sics.ktoolbox.aggregator.local.api.events;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfo;

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
