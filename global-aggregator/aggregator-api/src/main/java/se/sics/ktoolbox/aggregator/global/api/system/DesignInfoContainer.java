package se.sics.ktoolbox.aggregator.global.api.system;

import java.util.Collection;

/**
 * Container for processed window collection.
 * The container will be implemented by the application based on the
 * design processor.
 *
 * Created by babbar on 2015-09-04.
 */
public abstract class DesignInfoContainer <DI_I extends DesignInfo> {

    Collection<DI_I> processedWindows;

    public DesignInfoContainer(Collection<DI_I> processedWindows){
        this.processedWindows = processedWindows;
    }

    public Collection<DI_I> getProcessedWindows() {
        return processedWindows;
    }
}
