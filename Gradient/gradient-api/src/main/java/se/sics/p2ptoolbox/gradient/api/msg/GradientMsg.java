package se.sics.p2ptoolbox.gradient.api.msg;

import se.sics.kompics.KompicsEvent;

import java.util.UUID;

/**
 * Gradient Message wrapper class.
 *  
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientMsg {

    /**
     * One Way Message Abstract class.
     */
    public static abstract class OneWay implements KompicsEvent{
        
        public final UUID uuid;

        public OneWay(UUID uuid){
            this.uuid = uuid;
        }
        
        public UUID getUniqueId(){
            return this.uuid;
        }
    }
    
}
