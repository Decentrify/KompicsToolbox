/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.ktoolbox.aggregator.server.event;

import se.sics.ktoolbox.aggregator.server.util.ResponseMatcher;
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
        private int qty;
        private UUID requestId;

        public Request(UUID requestId, String designer, int startLoc, int qty){

            this.requestId = requestId;
            this.designer = designer;
            this.startLoc = startLoc;
            this.qty = qty;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "designer='" + designer + '\'' +
                    ", startLoc=" + startLoc +
                    ", qty=" + qty +
                    ", requestId=" + requestId +
                    '}';
        }

        public String getDesigner() {
            return designer;
        }

        public int getStartLoc() {
            return startLoc;
        }

        public int getQty() {
            return qty;
        }

        public UUID getRequestId() {
            return requestId;
        }
    }

    //TODO Alex - is DI_O supposed to extend DesignInfo ?
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
