package se.sics.ktoolbox.aggregator.global.api.event;

import com.google.common.base.Optional;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.aggregator.global.api.system.DesignInfo;
import se.sics.ktoolbox.aggregator.global.api.system.DesignInfoContainer;

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


    public static class Response extends ProcessingResponseMatcher {

        private UUID requestId;

        public Response(UUID requestId, DesignInfoContainer container) {
            super(container);
            this.requestId = requestId;
        }


        public Response(UUID requestId){
            super(null);
            this.requestId = requestId;
        }
    }

}
