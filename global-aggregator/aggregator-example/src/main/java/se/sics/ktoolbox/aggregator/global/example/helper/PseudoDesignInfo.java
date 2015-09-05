package se.sics.ktoolbox.aggregator.global.example.helper;

import se.sics.ktoolbox.aggregator.global.api.system.DesignInfo;

/**
 * Pseudo Design Information packets.
 * 
 * Created by babbarshaer on 2015-09-05.
 */
public class PseudoDesignInfo implements DesignInfo {
    
    private float averageSearchResponse;
    
    public PseudoDesignInfo(float searchResponse){
        this.averageSearchResponse = searchResponse;
    }
    
    public float getAverageSearchResponse(){
        return this.averageSearchResponse;
    }
    
}