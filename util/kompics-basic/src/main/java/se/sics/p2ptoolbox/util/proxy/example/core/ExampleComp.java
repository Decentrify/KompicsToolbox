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
package se.sics.p2ptoolbox.util.proxy.example.core;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ConfigurationException;
import se.sics.kompics.ControlPort;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.p2ptoolbox.util.proxy.ComponentProxy;
import se.sics.p2ptoolbox.util.proxy.HookParent;
import se.sics.p2ptoolbox.util.proxy.Required;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExampleComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleComp.class);
    private String logPrefix = "";

    private Positive<PortX> portX;
    private Negative<PortY> portY;

    private final HookTracker hookTracker;

    public ExampleComp(ExampleInit init) {
        LOG.info("{}initiating...");
        this.hookTracker = new HookTracker(init.hookDefinition);
        hookTracker.setupHook(true);
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleHPMsg, portX);
    }

    //***************************CONTROL****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            hookTracker.startHook(true);
            trigger(new HPMsg.Y(), portY);
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{}stopping...", logPrefix);
            hookTracker.preStop();
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        UUID compId = fault.getSourceCore().id();
        LOG.error("{}fault on comp:{}", new Object[]{logPrefix, compId});
        LOG.error("{}fault:{}", new Object[]{logPrefix, fault.getCause()});

        hookTracker.restart(false);
        trigger(new HPMsg.Y(), portY);

        return Fault.ResolveAction.RESOLVED;
    }

    //**************************************************************************
    Handler handleHPMsg = new Handler<HPMsg.X>() {
        @Override
        public void handle(HPMsg.X msg) {
            LOG.info("{}hp msg", logPrefix);
        }
    };

    //**************************HOOK_PARENT*************************************
    public class HookTracker {

        private HookXY.Definition hookDefinition;
        private HookXY.SetupResult hookSetup;
        private Component[] hook;

        public HookTracker(HookXY.Definition hookDefinition) {
            this.hookDefinition = hookDefinition;
        }

        private void setupHook(boolean setup) {
            UUID hookId = UUID.randomUUID();
            LOG.info("{}setting up hook", new Object[]{logPrefix});
            hookSetup = hookDefinition.setup(new HookParentProxy(), null, new HookXY.SetupInit(setup));
            hook = hookSetup.components;
            portX = hookSetup.portX;
            portY = hookSetup.portY;
        }
        
        private void startHook(boolean started) {
            hookDefinition.start(new HookParentProxy(), null, hookSetup, new HookXY.StartInit(started));
        }

        private void preStop() {
            LOG.info("{}tearing down hook", new Object[]{logPrefix});
            hookDefinition.preStop(new HookParentProxy(), null, hookSetup, new HookXY.TearInit(hook));
            hook = null;
        }
        
        private void restart(boolean setup) {
            preStop();
            setupHook(setup);
            startHook(false);
        }
    }
    
    public class ExampleHookParent implements HookParent {
    }

    public class HookParentProxy implements ComponentProxy {
    @Override
        public <P extends PortType> void trigger(KompicsEvent e, Port<P> p) {
            ExampleComp.this.trigger(e, p);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
            return ExampleComp.this.create(definition, initEvent);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
            return ExampleComp.this.create(definition, initEvent);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
            return ExampleComp.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
            return ExampleComp.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
            ExampleComp.this.disconnect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
            ExampleComp.this.disconnect(negative, positive);
        }

        @Override
        public Negative<ControlPort> getControlPort() {
            return ExampleComp.this.control;
        }

        @Override
        public <P extends PortType> Positive<P> requires(Class<P> portType) {
            return ExampleComp.this.requires(portType);
        }

        @Override
        public <P extends PortType> Negative<P> provides(Class<P> portType) {
            return ExampleComp.this.provides(portType);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelFilter filter) {
            return ExampleComp.this.connect(negative, positive, filter);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive, ChannelFilter filter) {
            return ExampleComp.this.connect(positive, negative, filter);
        }

        @Override
        public <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) throws ConfigurationException {
            ExampleComp.this.subscribe(handler, port);
        }
    }

    public static enum RequiredHook implements Required.Hook {
        A_B
    }

    public static class ExampleInit extends Init<ExampleComp> {

        public final HookXY.Definition hookDefinition;

        public ExampleInit(HookXY.Definition hookDefinition) {
            this.hookDefinition = hookDefinition;
        }
    }
    }
