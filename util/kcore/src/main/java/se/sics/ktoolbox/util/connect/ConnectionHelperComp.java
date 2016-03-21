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
package se.sics.ktoolbox.util.connect;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.PortType;
import se.sics.kompics.Start;
import se.sics.kompics.UniDirectionalChannel;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnectionHelperComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionHelperComp.class);
    private String logPrefix = "";

    public ConnectionHelperComp(ConnectionHelperInit init) {
        LOG.info("{}initiating...", logPrefix);
        subscribe(handleStart, control);
        for (Class<PortType> port : init.proxyPorts) {
//        Handler handlePos = new Handler<ContentMsg>() {
//            @Override
//            public void handle(ContentMsg msg) {
//                LOG.trace("forwarding pos msg:{} from:{} to:{}", new Object[]{msg.getContent(), 
//                    msg.getHeader().getSource(), msg.getHeader().getDestination()});
//                trigger(msg, neg);
//            }
//        };
//        Handler handleNeg = new Handler<ContentMsg>() {
//            @Override
//            public void handle(ContentMsg msg) {
//                LOG.info("forwarding neg msg:{} from:{} to:{}", new Object[]{msg.getContent(),
//                    msg.getHeader().getSource(), msg.getHeader().getDestination()});
//                trigger(msg, pos);
//            }
//        };
//        subscribe(handlePos, pos);
//        subscribe(handleNeg, neg);
            connect(requires(port), provides(port), UniDirectionalChannel.TWO_WAY);
        }
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    public static class ConnectionHelperInit extends Init<ConnectionHelperComp> {

        public final Set<Class> proxyPorts;

        public ConnectionHelperInit(Set<Class> proxyPorts) {
            this.proxyPorts = proxyPorts;
        }
    }
}
