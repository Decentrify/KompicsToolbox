package se.sics.ktoolbox.aggregator.global.api.event;

import com.google.common.base.Optional;
import se.sics.ktoolbox.aggregator.global.api.system.DesignInfo;
import se.sics.ktoolbox.aggregator.global.api.system.DesignInfoContainer;

/**
 * Response Matcher for a simple window processing response.
 *
 * Created by babbarshaer on 2015-09-05.
 */
public abstract class ProcessingResponseMatcher<DI_O extends DesignInfo> implements ResponseMatcher<DesignInfoContainer<DI_O>> {

    public DesignInfoContainer<DI_O> container;

    public ProcessingResponseMatcher(DesignInfoContainer<DI_O> container){
        this.container = container;
    }

    public DesignInfoContainer<DI_O> getContent() {
        return this.container;
    }

    public Class<DesignInfoContainer<DI_O>> extractPattern() {
        return (Class<DesignInfoContainer<DI_O>>)this.container.getClass();
    }

    public DesignInfoContainer<DI_O> extractValue() {
        return this.container;
    }
}