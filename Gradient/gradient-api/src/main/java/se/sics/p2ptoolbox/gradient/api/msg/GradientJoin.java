package se.sics.p2ptoolbox.gradient.api.msg;

import se.sics.gvod.net.VodAddress;

import java.util.Set;
import java.util.UUID;

/**
 * Join Message event sent to boot up the gradient service.
 *
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientJoin extends GradientMsg.OneWay {

    private final Set<VodAddress> peers;

    public GradientJoin(UUID uuid, Set<VodAddress> peers) {
        super(uuid);
        this.peers = peers;
    }
    
    public Set<VodAddress> getBootstrapNodes(){
        return this.peers;
    }
    
}
