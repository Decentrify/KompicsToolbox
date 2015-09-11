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
import se.sics.p2ptoolbox.util.proxy.Required;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HookParentComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(HookParentComp.class);
    private String logPrefix = "";

    private Positive<PortX> portX;
    private Negative<PortY> portY;

    private final HookTracker hookTracker;

    public HookParentComp(HookParentInit init) {
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        this.hookTracker = new HookTracker(init.hookDefinition);
    }

    //***************************CONTROL****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            hookTracker.setupHook(true);
            subscribe(handleHPMsg, portX);
            trigger(new HPMsg.Y(), portY);
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{}stopping...", logPrefix);
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        UUID compId = fault.getSourceCore().id();
        LOG.error("{}fault on comp:{}", new Object[]{logPrefix, compId});
        LOG.error("{}fault:{}", new Object[]{logPrefix, fault.getCause()});

        hookTracker.tearDown(compId);
        hookTracker.setupHook(false);
        trigger(new HPMsg.Y(), portY);

        return Fault.ResolveAction.RESOLVED;
    }

    //**************************************************************************
    Handler handleHPMsg = new Handler<HPMsg.X>() {
        @Override
        public void handle(HPMsg.X msg) {
            LOG.info("hp msg");
        }
    };

    //**************************HOOK_PARENT*************************************
    public class HookTracker implements ComponentProxy {

        private HookXY.Definition hookDefinition;
        private Component[] hook;

        public HookTracker(HookXY.Definition hookDefinition) {
            this.hookDefinition = hookDefinition;
        }

        private void setupHook(boolean setup) {
            UUID hookId = UUID.randomUUID();
            LOG.info("{}setting up hook", new Object[]{logPrefix});
            HookXY.InitResult result = hookDefinition.setUp(this, new HookXY.Init(setup));
            hook = result.components;
            portX = result.portX;
            portY = result.portY;
        }

        private void tearDown(UUID compId) {
            LOG.info("{}tearing down hook", new Object[]{logPrefix});

            hookDefinition.tearDown(this, new HookXY.Tear(hook));
            hook = null;
        }

        //*******************************PROXY**********************************
        @Override
        public <P extends PortType> void trigger(KompicsEvent e, Port<P> p) {
            HookParentComp.this.trigger(e, p);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
            return HookParentComp.this.create(definition, initEvent);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
            return HookParentComp.this.create(definition, initEvent);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
            return HookParentComp.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
            return HookParentComp.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
            HookParentComp.this.disconnect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
            HookParentComp.this.disconnect(negative, positive);
        }

        @Override
        public Negative<ControlPort> getControlPort() {
            return HookParentComp.this.control;
        }

        @Override
        public <P extends PortType> Positive<P> requires(Class<P> portType) {
            return HookParentComp.this.requires(portType);
        }

        @Override
        public <P extends PortType> Negative<P> provides(Class<P> portType) {
            return HookParentComp.this.provides(portType);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelFilter filter) {
            return HookParentComp.this.connect(negative, positive, filter);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive, ChannelFilter filter) {
            return HookParentComp.this.connect(positive, negative, filter);
        }
    }

    public static enum RequiredHook implements Required.Hook {
        A_B
    }

    public static class HookParentInit extends Init<HookParentComp> {

        public final HookXY.Definition hookDefinition;

        public HookParentInit(HookXY.Definition hookDefinition) {
            this.hookDefinition = hookDefinition;
        }
    }
}
