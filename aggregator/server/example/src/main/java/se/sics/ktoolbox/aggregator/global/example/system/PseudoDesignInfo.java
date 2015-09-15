package se.sics.ktoolbox.aggregator.global.example.system;

import se.sics.ktoolbox.aggregator.server.api.system.DesignInfo;

/**
 * Pseudo Design Information packets.
 * 
 * Created by babbarshaer on 2015-09-05.
 */
public class PseudoDesignInfo implements DesignInfo {
    
    private float averageSearchResponse;
    private int count;
    
    public PseudoDesignInfo(float searchResponse, int count){
        this.averageSearchResponse = searchResponse;
        this.count = count;
    }
    
    public float getAverageSearchResponse(){
        return this.averageSearchResponse;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "PseudoDesignInfo{" +
                "averageSearchResponse=" + averageSearchResponse +
                ", count=" + count +
                '}';
    }
}