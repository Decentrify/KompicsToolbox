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
package se.sics.ktoolbox.util.address.nat.example;

import com.google.common.base.Optional;
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
import se.sics.ktoolbox.util.address.nat.example.msg.AppMsg;
import se.sics.ktoolbox.util.msg.BasicContentMsg;
import se.sics.ktoolbox.util.msg.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AppComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(AppComp.class);
    private String logPrefix = "";

    private final Positive network = requires(Network.class);

    private Address self;
    private Optional<Address> target;

    public AppComp(AppInit init) {
        LOG.info("{}initiating...", logPrefix);

        self = init.self;
        target = init.target;

        subscribe(handleStart, control);
        subscribe(handlePing, network);
        subscribe(handlePong, network);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            if (target.isPresent()) {
                BasicHeader header = new BasicHeader(self, target.get(), Transport.UDP);
                BasicContentMsg msg = new BasicContentMsg(header, new AppMsg.Ping());
                LOG.info("{}sending ping from:{} to:{}", new Object[]{logPrefix, msg.getSource(), msg.getDestination()});
                trigger(msg, network);
            }
        }
    };

    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<AppMsg.Ping, BasicContentMsg<Address, Header<Address>, AppMsg.Ping>>() {

                @Override
                public void handle(AppMsg.Ping content, BasicContentMsg<Address, Header<Address>, AppMsg.Ping> container) {
                    LOG.info("{}received ping from:{} on:{}", new Object[]{logPrefix, container.getSource(), container.getDestination()});
                    BasicHeader header = new BasicHeader(self, container.getSource(), Transport.UDP);
                    BasicContentMsg msg = new BasicContentMsg(header, new AppMsg.Pong());
                    LOG.info("{}sending pong from:{} to:{}", new Object[]{logPrefix, msg.getSource(), msg.getDestination()});
                    trigger(msg, network);
                }
            };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<AppMsg.Pong, BasicContentMsg<Address, Header<Address>, AppMsg.Pong>>() {

                @Override
                public void handle(AppMsg.Pong content, BasicContentMsg<Address, Header<Address>, AppMsg.Pong> container) {
                    LOG.info("{}received pong from:{} on:{}", new Object[]{logPrefix, container.getSource(), container.getDestination()});
                }
            };

    public static class AppInit extends Init<AppComp> {

        public final Address self;
        public final Optional<Address> target;

        public AppInit(Address self, Optional<Address> target) {
            this.self = self;
            this.target = target;
        }
    }
}
