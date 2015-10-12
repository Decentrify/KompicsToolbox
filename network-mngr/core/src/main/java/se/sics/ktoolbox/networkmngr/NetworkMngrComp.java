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
package se.sics.ktoolbox.networkmngr;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.networkmngr.events.Connect;
import se.sics.ktoolbox.networkmngr.events.Create;
import se.sics.ktoolbox.networkmngr.events.Disconnect;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.proxy.SystemHookSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkMngrComp.class);
    private String logPrefix = "";

    private KConfigCache config;
    private Negative<NetworkMngrPort> mngrPort = provides(NetworkMngrPort.class);
    
    private Positive<Network> network = requires(Network.class);
    private final SystemHookSetup systemHooks;
//    private Map<UUID, Pair<Integer, HookId>> activeNetworks = new HashMap<>();
    private Map<UUID, Integer> networkReferences = new HashMap<>();
    
    public NetworkMngrComp(NetworkMngrInit init) {
        this.config = init.config;
        this.logPrefix = config.getNodeId() + " ";
        LOG.info("{}initiating...", logPrefix);
        
        this.systemHooks =  init.systemHooks;
        
        subscribe(handleStart, control);    
        subscribe(handleCreate, mngrPort);
        subscribe(handleConnect, mngrPort);
        subscribe(handleDisconnect, mngrPort);
    }
    
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };
    
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.error("{}one of the networks faulted", logPrefix);
        return Fault.ResolveAction.ESCALATE;
    }
    
    Handler handleCreate = new Handler<Create.Request>() {
        @Override
        public void handle(Create.Request req) {
//            LOG.debug("{}received start request for:{} on:{}",
//                    new Object[]{logPrefix, req.hookId, req.self});
            
        }
    };
    
    Handler handleConnect = new Handler<Connect.Request>() {
        @Override
        public void handle(Connect.Request event) {
        }
    };

    Handler handleDisconnect = new Handler<Disconnect.Request>() {
        @Override
        public void handle(Disconnect.Request event) {
        }
    };
    
    public static class NetworkMngrInit extends Init<NetworkMngrComp> {
        public final KConfigCache config;
        public final SystemHookSetup systemHooks;
        
        public NetworkMngrInit(KConfigCore config, SystemHookSetup systemHooks) {
            this.config = new KConfigCache(config);
            this.systemHooks = systemHooks;
        }
    }
}
