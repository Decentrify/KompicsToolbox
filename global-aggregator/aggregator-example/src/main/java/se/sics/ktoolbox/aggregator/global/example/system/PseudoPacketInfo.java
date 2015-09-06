package se.sics.ktoolbox.aggregator.global.example.system;

import se.sics.ktoolbox.aggregator.global.api.system.PacketInfo;

/**
 * Pseudo Packet Information.
 *  
 * Created by babbarshaer on 2015-09-05.
 */
public class PseudoPacketInfo implements PacketInfo{
    
    private float response;
    private float price;
    
    public PseudoPacketInfo(float response, float price){
        
        this.response = response;
        this.price = price;
    }
    
    public float getResponse(){
        return this.response;
    }
    
    public float getPrice(){
        return this.price;
    }
    
    @Override
    public String toString() {
        return "PseudoPacketInfo{" +
                "response=" + response +
                ", price=" + price +
                '}';
    }
}
