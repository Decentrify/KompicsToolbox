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
package se.sics.ledbat.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ledbat.ncore.msg.LedbatMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Seeder extends ComponentDefinition {
    private final static Logger LOG = LoggerFactory.getLogger(Seeder.class);
    private String logPrefix = "";

    //**************************************************************************
    Positive<Network> networkPort = requires(Network.class);
    //**************************************************************************
    private final byte[] payload;

    public Seeder(Init init) {
        this.payload = init.payload;
        subscribe(handleStart, control);
        subscribe(handleMsgRequest, networkPort);
    }

    Handler handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    ClassMatchedHandler handleMsgRequest
            = new ClassMatchedHandler<LedbatMsg.Request<ExMsg.Request>, KContentMsg<KAddress, KHeader<KAddress>, LedbatMsg.Request<ExMsg.Request>>>() {

                @Override
                public void handle(LedbatMsg.Request<ExMsg.Request> content, KContentMsg<KAddress, KHeader<KAddress>, LedbatMsg.Request<ExMsg.Request>> context) {
                    trigger(context.answer(content.answer(content.getWrappedContent().answer(payload))), networkPort);
                }
            };

    public static class Init extends se.sics.kompics.Init<Seeder> {

        public final byte[] payload;

        public Init(byte[] payload) {
            this.payload = payload;
        }
    }
}
