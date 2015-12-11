/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.UniDirectionalChannel;
import se.sics.kompics.config.Config;
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.util.proxy.Hook;
import se.sics.ktoolbox.util.proxy.SystemHookSetup;
import se.sics.ktoolbox.util.proxy.example.core.BaseComp.BaseInit;
import se.sics.ktoolbox.util.proxy.network.NetworkHook;
import se.sics.ktoolbox.util.config.KConfigCore;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExampleComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleComp.class);
    private String logPrefix = "";

    private final SystemKCWrapper systemConfig;
    private final ExampleKCWrapper exampleConfig;
    private final SystemHookSetup systemHooks;

    private final NetworkHook.Parent networkParent;
    private NetworkHook.SetupResult networkSetup;
    private Component baseComp;
    private final XYHook.Parent xyParent;
    private XYHook.SetupResult xySetup;

    public ExampleComp(ExampleInit init) {
        systemConfig = new SystemKCWrapper(init.configCore);
        exampleConfig = new ExampleKCWrapper(init.configCore);
        systemHooks = init.systemHooks;
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initiating...", logPrefix);

        networkParent = new NetworkHook.Parent() {

            @Override
            public void on(UUID id) {
                boolean started = false;
                setupBase();
                setupXYHook(exampleConfig.hookFail);
                startBase(started);
                startXYHook(started);
            }

            @Override
            public void off(UUID id) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        xyParent = new XYHook.Parent() {
            @Override
            public void on() {
                if (exampleConfig.sendHPMsg) {
                    trigger(new ExampleEvent.X(), xySetup.portX);
                } else {
                    subscribe(handleX, xySetup.portX);
                }
            }
        };
        setupNetwork();

        subscribe(handleStart, control);
    }

    //***************************CONTROL****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            startNetwork(true);
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        UUID compId = fault.getSourceCore().id();
        LOG.error("{}fault on comp:{}", new Object[]{logPrefix, compId});
        LOG.error("{}fault:{}", new Object[]{logPrefix, fault.getCause()});

        restartXYHook(false);
        return Fault.ResolveAction.RESOLVED;
    }

    Handler handleX = new Handler<ExampleEvent.X>() {
        @Override
        public void handle(ExampleEvent.X event) {
            LOG.info("{}received:{}", logPrefix, event);
        }
    };
    //**************************************************************************
    private void setupNetwork() {
        LOG.info("{}setting up network", new Object[]{logPrefix});
        NetworkHook.Definition networkDefinition = systemHooks.getHook(RequiredHooks.NETWORK_HOOK.name(), NetworkHook.Definition.class);
        networkSetup = networkDefinition.setup(proxy, networkParent, new NetworkHook.SetupInit(UUID.randomUUID(), exampleConfig.localAdr));
    }

    private void startNetwork(boolean started) {
        LOG.info("{}starting network", new Object[]{logPrefix});
        NetworkHook.Definition networkDefinition = systemHooks.getHook(RequiredHooks.NETWORK_HOOK.name(), NetworkHook.Definition.class);
        networkDefinition.start(proxy, networkParent, networkSetup, new NetworkHook.StartInit(started));
    }

    private void setupBase() {
        LOG.info("{}setting up base comp", new Object[]{logPrefix});
        baseComp = create(BaseComp.class, new BaseInit(exampleConfig.localAdr, exampleConfig.partnerAdr));
        connect(networkSetup.network, baseComp.getNegative(Network.class), UniDirectionalChannel.TWO_WAY);
    }

    private void startBase(boolean started) {
        LOG.info("{}starting base comp", new Object[]{logPrefix});
        if (!started) {
            trigger(Start.event, baseComp.control());
        }
    }

    private void setupXYHook(boolean hookFail) {
        LOG.info("{}setting up xyhook", new Object[]{logPrefix});
        XYHook.Definition hookDefinition = systemHooks.getHook(RequiredHooks.XY_HOOK.name(), XYHook.Definition.class);
        xySetup = hookDefinition.setup(proxy, xyParent, new XYHook.SetupInit(hookFail));
        
        connect(baseComp.getPositive(PortY.class), xySetup.portY, UniDirectionalChannel.TWO_WAY);
    }

    private void startXYHook(boolean started) {
        LOG.info("{}starting xyhook", new Object[]{logPrefix});
        XYHook.Definition hookDefinition = systemHooks.getHook(RequiredHooks.XY_HOOK.name(), XYHook.Definition.class);
        hookDefinition.start(proxy, xyParent, xySetup, new XYHook.StartInit(started));
    }

    private void killHook(boolean killed) {
        LOG.info("{}tearing down xyhook", new Object[]{logPrefix});
        XYHook.Definition hookDefinition = systemHooks.getHook(RequiredHooks.XY_HOOK.name(), XYHook.Definition.class);
        hookDefinition.tearDown(ExampleComp.this.proxy, xyParent, xySetup, new XYHook.TearInit(killed));
        xySetup = null;
    }

    private void restartXYHook(boolean setup) {
        killHook(false);
        setupXYHook(setup);
        startXYHook(false);
    }

    public static enum RequiredHooks implements Hook.Required {

        NETWORK_HOOK, XY_HOOK;
    }

    public static class ExampleInit extends Init<ExampleComp> {

        public final KConfigCore configCore;
        public final SystemHookSetup systemHooks;

        public ExampleInit(KConfigCore configCore, SystemHookSetup systemHooks) {
            this.configCore = configCore;
            this.systemHooks = systemHooks;
        }
    }
}