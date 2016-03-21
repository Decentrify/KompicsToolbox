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
package se.sics.ktoolbox.util.proxy.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.ktoolbox.util.msg.BasicContentMsg;
import se.sics.ktoolbox.util.msg.BasicHeader;
import se.sics.ktoolbox.util.msg.DecoratedHeader;
import se.sics.ktoolbox.util.update.view.ViewUpdate;
import se.sics.ktoolbox.util.update.view.impl.OverlayView;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BaseComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(BaseComp.class);
    private String logPrefix = "";

    private final Negative portY = provides(PortY.class);
    private final Positive network = requires(Network.class);

    private final Address localAdr;
    private final Address partnerAdr;

    public BaseComp(BaseInit init) {
        LOG.info("{}initiating...", logPrefix);

        localAdr = init.localAdr;
        partnerAdr = init.partnerAdr;

        subscribe(handleStart, control);
        subscribe(handleMsgOut, portY);
        subscribe(handleMsgIn, network);
    }

    //***************************CONTROL****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    //**************************************************************************
    ClassMatchedHandler handleMsgIn
            = new ClassMatchedHandler<ExampleEvent.Y, BasicContentMsg<Address, Header<Address>, ExampleEvent.Y>>() {

                @Override
                public void handle(ExampleEvent.Y content, BasicContentMsg<Address, Header<Address>, ExampleEvent.Y> container) {
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource()});
                    trigger(content, portY);
                }
            };

    Handler handleMsgOut = new Handler<ExampleEvent.Y>() {
        @Override
        public void handle(ExampleEvent.Y event) {
            DecoratedHeader requestHeader
                    = new DecoratedHeader(new BasicHeader(localAdr, partnerAdr, Transport.UDP), null, null);
            BasicContentMsg request = new BasicContentMsg(requestHeader, event);
            LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, event, partnerAdr});
            trigger(request, network);
        }
    };

    public static class BaseInit extends Init<BaseComp> {

        public final Address localAdr;
        public final Address partnerAdr;

        public BaseInit(Address localAdr, Address partnerAdr) {
            this.localAdr = localAdr;
            this.partnerAdr = partnerAdr;
        }
    }
}
