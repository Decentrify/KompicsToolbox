package se.sics.ktoolbox.aggregator.global.example.system;

import se.sics.ktoolbox.aggregator.global.api.system.DesignInfoContainer;

import java.util.Collection;

/**
 * Container for the another design information.
 * Created by babbar on 2015-09-06.
 */
public class AnotherDesignInfoContainer extends DesignInfoContainer<AnotherDesignInfo>{

    public AnotherDesignInfoContainer(Collection<AnotherDesignInfo> processedWindows) {
        super(processedWindows);
    }

    @Override
    public String toString(){
        return super.toString();
    }
}
