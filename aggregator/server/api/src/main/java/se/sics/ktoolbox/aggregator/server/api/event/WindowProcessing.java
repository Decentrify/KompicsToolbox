package se.sics.ktoolbox.aggregator.server.api.event;

import se.sics.kompics.KompicsEvent;

import java.util.UUID;

/**
 * Event indicating the processing of the windows
 * being held by the visualizer.
 *
 * Created by babbar on 2015-09-04.
 */
public class WindowProcessing {


    public static class Request implements KompicsEvent{

        private String designer;
        private int startLoc;
        private int endLoc;
        private UUID requestId;

        public Request(UUID requestId, String designer, int startLoc, int endLoc){

            this.requestId = requestId;
            this.designer = designer;
            this.startLoc = startLoc;
            this.endLoc = endLoc;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "designer='" + designer + '\'' +
                    ", startLoc=" + startLoc +
                    ", endLoc=" + endLoc +
                    ", requestId=" + requestId +
                    '}';
        }

        public String getDesigner() {
            return designer;
        }

        public int getStartLoc() {
            return startLoc;
        }

        public int getEndLoc() {
            return endLoc;
        }

        public UUID getRequestId() {
            return requestId;
        }
    }


    public static class Response<DI_O> implements ResponseMatcher<DI_O>, KompicsEvent{

        DI_O container;
        UUID requestId;
        
        public Response(UUID requestId, DI_O container){
            this.container = container;
            this.requestId = requestId;
        }

        public UUID getRequestId() {
            return requestId;
        }

        @Override
        public DI_O getContent() {
            return this.container;
        }

        @Override
        public Class<DI_O> extractPattern() {
            return (Class<DI_O>) this.container.getClass();
        }

        @Override
        public DI_O extractValue() {
            return this.container;
        }
    }

}
