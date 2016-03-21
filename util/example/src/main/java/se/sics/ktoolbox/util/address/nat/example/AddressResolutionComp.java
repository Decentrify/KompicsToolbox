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

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.util.address.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.address.nat.NatType;
import se.sics.ktoolbox.util.msg.ContentMsg;
import se.sics.ktoolbox.util.msg.Header2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AddressResolutionComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(AddressResolutionComp.class);
    private String logPrefix = "";

    private final Negative inNetwork = provides(Network.class);
    private final Positive outNetwork = requires(Network.class);

    //<nodeId, port>
    private final Map<Integer, Integer> mappings = new HashMap<>();

    public AddressResolutionComp(AddressResolutionInit init) {
        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleMsgOut, inNetwork);
        subscribe(handleMsgIn, outNetwork);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    Handler handleMsgOut
            = new Handler<ContentMsg<NatAwareAddressImpl, Header2<NatAwareAddressImpl>, Object>>() {

                @Override
                public void handle(ContentMsg<NatAwareAddressImpl, Header2<NatAwareAddressImpl>, Object> msg) {
                    NatAwareAddressImpl dst = msg.getHeader().getDestination();
                    if (NatType.isNated(dst)) {
                        if (dst.getPort() == 0) {
                            Integer targetMapping = mappings.get(dst.getId());
                            NatAwareAddressImpl newSource = msg.getHeader().getSource().withPublicPort(selfPublicPort);
                            NatAwareAddressImpl newDestination = msg.getHeader().getDestination().withPublicPort(targetPublicPort);
                            Header2<NatAwareAddressImpl> newHeader = msg.getHeader().withSourceDestination(newSource, newDestination);
                            ContentMsg<NatAwareAddressImpl, Header2<NatAwareAddressImpl>, Object> newMsg = msg.withHeader(newHeader);
                            LOG.trace("{}sending:{} from private:{} public:{} to:{}",
                                    new Object[]{logPrefix, newMsg.getContent(), newMsg.getHeader().getSource().getPrivateAdr().get(),
                                        newMsg.getHeader().getSource().getPublicAdr(), newMsg.getHeader().getDestination().getPublicAdr()});
                            trigger(newMsg, outNetwork);
                        } else {
                            throw new RuntimeException("nated dst should have port 0");
                        }
                    }
                }
            };

    Handler handleMsgIn
            = new Handler<ContentMsg<NatAwareAddressImpl, Header2<NatAwareAddressImpl>, Object>>() {

                @Override
                public void handle(ContentMsg<NatAwareAddressImpl, Header2<NatAwareAddressImpl>, Object> msg) {
                    LOG.trace("{}received:{} from private:{} public:{} to:{}",
                            new Object[]{logPrefix, msg.getContent(), msg.getHeader().getSource().getPrivateAdr().get(),
                                msg.getHeader().getSource().getPublicAdr(), msg.getHeader().getDestination().getPublicAdr()});
                    NatAwareAddressImpl newSource = msg.getHeader().getSource().withPublicPort(0);
                    NatAwareAddressImpl newDestination = msg.getHeader().getDestination().withPublicPort(0);
                    Header2<NatAwareAddressImpl> newHeader = msg.getHeader().withSourceDestination(newSource, newDestination);
                    ContentMsg<NatAwareAddressImpl, Header2<NatAwareAddressImpl>, Object> newMsg = msg.withHeader(newHeader);
                    trigger(newMsg, inNetwork);
                }
            };

    public static class AddressResolutionInit extends Init<AddressResolutionComp> {

        public AddressResolutionInit() {
        }
    }
}
