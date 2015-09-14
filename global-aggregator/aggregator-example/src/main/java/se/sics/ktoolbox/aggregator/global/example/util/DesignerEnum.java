package se.sics.ktoolbox.aggregator.global.example.util;

/**
 *
 * Created by babbarshaer on 2015-09-06.
 */
public enum DesignerEnum {
    
    PSEUDO("pseudo");

    private String val;
    private DesignerEnum(String val){
        this.val = val;
    }
    
    public String getVal(){
        return this.val;
    }
    
}
