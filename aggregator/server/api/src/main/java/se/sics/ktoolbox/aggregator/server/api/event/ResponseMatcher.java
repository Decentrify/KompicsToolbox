package se.sics.ktoolbox.aggregator.server.api.event;

import se.sics.kompics.PatternExtractor;

/**
 * Interface indicating the processing response for the
 *
 * Created by babbar on 2015-09-04.
 */
public interface ResponseMatcher<Content extends Object> extends PatternExtractor<Class<Content>, Content>{
    public Content getContent();
}

