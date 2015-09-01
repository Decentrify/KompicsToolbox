package se.sics.ktoolbox.aggregator.local.example.util;

import se.sics.ktoolbox.aggregator.local.api.ComponentInfo;

/**
 * Application component information object.
 * Created by babbarshaer on 2015-08-31.
 */
public class AppComponentInfo implements ComponentInfo {

    public final Integer var1;
    public final Integer var2;
    public final String str;

    public AppComponentInfo(Integer it1, Integer it2, String str){
        this.var1 = it1;
        this.var2 = it2;
        this.str = str;
    }

    @Override
    public String toString() {
        return "AppComponentInfo{" +
                "var1=" + var1 +
                ", var2=" + var2 +
                ", str='" + str + '\'' +
                '}';
    }
}
