package se.sics.ktoolbox.aggregator.local.example.util;

import se.sics.ktoolbox.aggregator.local.api.PacketInfo;

/**
 * PacketInformation containing the integer pair from
 * the application state.
 *
 * Created by babbarshaer on 2015-08-31.
 */
public class IntegerPacketInfo implements PacketInfo{

    public final Integer var1;
    public final Integer var2;

    public IntegerPacketInfo(Integer it1, Integer it2){
        this.var1  = it1;
        this.var2  = it2;
    }

}
