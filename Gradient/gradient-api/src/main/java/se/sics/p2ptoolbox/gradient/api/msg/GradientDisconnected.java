package se.sics.p2ptoolbox.gradient.api.msg;

import java.util.UUID;

/**
 * Indication Event sent by gradient in case it could not
 * find any node to shuffle with.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientDisconnected  extends GradientMsg.OneWay{
    
    public final int overlayId;
    
    public GradientDisconnected(UUID uuid, int overlayId) {
        super(uuid);
        this.overlayId = overlayId;
    }
    
    public int getOverlayId(){
        return this.overlayId;
    }
    
}
