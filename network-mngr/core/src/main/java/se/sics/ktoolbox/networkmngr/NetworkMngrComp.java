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

import com.google.common.base.Optional;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.ipsolver.hooks.IpSolverHook;
import se.sics.ktoolbox.ipsolver.hooks.IpSolverResult;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ktoolbox.networkmngr.events.Bind;
import se.sics.ktoolbox.networkmngr.hooks.NetworkHook;
import se.sics.ktoolbox.networkmngr.hooks.NetworkResult;
import se.sics.ktoolbox.networkmngr.hooks.PortBindingHook;
import se.sics.ktoolbox.networkmngr.hooks.PortBindingResult;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.proxy.ComponentProxy;
import se.sics.p2ptoolbox.util.proxy.SystemHookSetup;
import se.sics.p2ptoolbox.util.truefilters.SourcePortFilter;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkMngrComp.class);
    private String logPrefix = "";

    private Negative<NetworkMngrPort> manager = provides(NetworkMngrPort.class);
    private Negative<Network> network = provides(Network.class);

    private final KConfigCache config;
    private final SystemHookSetup systemHooks;

    private InetAddress localIp;
    private final Map<UUID, NetworkParent> netComponents = new HashMap<>();

    public NetworkMngrComp(NetworkMngrInit init) {
        this.config = init.config;
        this.logPrefix = config.getNodeId() + " ";
        LOG.info("{}initiating...", logPrefix);

        this.systemHooks = init.systemHooks;

        subscribe(handleStart, control);
        subscribe(handleCreate, manager);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            IpSolverParent ipSolverParent = new IpSolverParent();
            ipSolverParent.solveLocalIp(true);
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.error("{}one of the networks faulted", logPrefix);
        return Fault.ResolveAction.ESCALATE;
    }

    //*******************************IP*****************************************
    private class IpSolverParent implements IpSolverHook.Parent {

        private IpSolverHook.SetupResult ipSolverSetup;

        private void solveLocalIp(boolean started) {
            IpSolverHook.Definition ipSolver = systemHooks.getHook(NetworkMngrHooks.RequiredHooks.IP_SOLVER.hookName,
                    NetworkMngrHooks.IP_SOLVER_HOOK);
            ipSolverSetup = ipSolver.setup(new NetworkMngrProxy(), this, new IpSolverHook.SetupInit());
            Optional<String> prefferedInterface = config.read(NetworkMngrConfig.prefferedInterface);
            List<GetIp.NetworkInterfacesMask> prefferedInterfaces = getPrefferedInterfaces();
            ipSolver.start(new NetworkMngrProxy(), this, ipSolverSetup, new IpSolverHook.StartInit(started, prefferedInterface, prefferedInterfaces));
        }

        private List<GetIp.NetworkInterfacesMask> getPrefferedInterfaces() {
            Optional<List> prefferedMasks = config.read(NetworkMngrConfig.prefferedMasks);
            List<GetIp.NetworkInterfacesMask> prefferedInterfaces = new ArrayList<>();
            if (!prefferedMasks.isPresent()) {
                prefferedInterfaces.add(GetIp.NetworkInterfacesMask.ALL);
            } else {
                prefferedInterfaces.addAll(prefferedMasks.get());
            }
            return prefferedInterfaces;
        }

        @Override
        public void onResult(IpSolverResult result) {
            if(!result.getIp().isPresent()) {
                LOG.error("{}could not get any ip", logPrefix);
                throw new RuntimeException("could not get any ip");
            }
            NetworkMngrComp.this.localIp = result.getIp().get();
            config.write(NetworkMngrConfig.localIp, result.getIp().get().getHostAddress());
        }
    }

    //***************************NETWORK, PORT**********************************
    Handler handleCreate = new Handler<Bind.Request>() {
        @Override
        public void handle(Bind.Request req) {
            LOG.info("{}binding on:{} localIp:{}", new Object[]{logPrefix, req.self, localIp});
            Optional<Bind.Response> existing = getMapping(req);
            if (existing.isPresent()) {
                trigger(existing.get(), manager);
                return;
            }

            NetworkParent networkParent = new NetworkParent(UUID.randomUUID(), req);
            networkParent.bindPort(false);
            netComponents.put(networkParent.id, networkParent);
        }
    };

    private Optional<Bind.Response> getMapping(Bind.Request req) {
        //TODO check for requested port, and interfaces 
        return Optional.absent();
    }

    private class NetworkParent implements NetworkHook.Parent, PortBindingHook.Parent {

        final UUID id;
        final Bind.Request req;
        PortBindingHook.SetupResult portBindingSetup;
        PortBindingResult portBindingResult;
        NetworkHook.SetupResult networkSetup;
        NetworkResult networkResult;

        public NetworkParent(UUID id, Bind.Request req) {
            this.id = id;
            this.req = req;
        }

        void bindPort(boolean started) {
            PortBindingHook.Definition portBindingHook = systemHooks.getHook(
                    NetworkMngrHooks.RequiredHooks.PORT_BINDING.hookName, NetworkMngrHooks.PORT_BINDING_HOOK);
            portBindingSetup = portBindingHook.setup(new NetworkMngrProxy(), this,
                    new PortBindingHook.SetupInit());
            portBindingHook.start(new NetworkMngrProxy(), this, portBindingSetup,
                    new PortBindingHook.StartInit(started, localIp, req.self.getPort(), req.forceProvidedPort));
        }

        @Override
        public void onResult(PortBindingResult result) {
            this.portBindingResult = result;
            setNetwork(false);
        }

        void setNetwork(boolean started) {
            NetworkHook.Definition networkHook = systemHooks.getHook(
                    NetworkMngrHooks.RequiredHooks.NETWORK.hookName, NetworkMngrHooks.NETWORK_HOOK);
            DecoratedAddress adr = req.self.changePort(portBindingResult.boundPort);
            networkSetup = networkHook.setup(new NetworkMngrProxy(), this,
                    new NetworkHook.SetupInit(adr, Optional.of(localIp)));
            networkHook.start(new NetworkMngrProxy(), this, networkSetup, new NetworkHook.StartInit(started));
        }

        @Override
        public void onResult(NetworkResult result) {
            this.networkResult = result;
            connect(networkResult.getNetwork(), network, new SourcePortFilter(portBindingResult.boundPort, false));
            Bind.Response resp = req.answer(id, portBindingResult.boundPort);
            trigger(resp, manager);
        }
    }

    public class NetworkMngrProxy implements ComponentProxy {

        @Override
        public <P extends PortType> Positive<P> requires(Class<P> portType) {
            return NetworkMngrComp.this.requires(portType);
        }

        @Override
        public <P extends PortType> Negative<P> provides(Class<P> portType) {
            return NetworkMngrComp.this.provides(portType);
        }

        @Override
        public Negative<ControlPort> getControlPort() {
            return NetworkMngrComp.this.control;
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
            return NetworkMngrComp.this.create(definition, initEvent);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
            return NetworkMngrComp.this.create(definition, initEvent);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
            return NetworkMngrComp.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelFilter filter) {
            return NetworkMngrComp.this.connect(negative, positive, filter);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
            return NetworkMngrComp.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive, ChannelFilter filter) {
            return NetworkMngrComp.this.connect(negative, positive, filter);
        }

        @Override
        public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
            NetworkMngrComp.this.disconnect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
            NetworkMngrComp.this.disconnect(negative, positive);
        }

        @Override
        public <P extends PortType> void trigger(KompicsEvent e, Port<P> p) {
            NetworkMngrComp.this.trigger(e, p);
        }

        @Override
        public <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) throws ConfigurationException {
            NetworkMngrComp.this.subscribe(handler, port);
        }

    }

    public static class NetworkMngrInit extends Init<NetworkMngrComp> {

        public final KConfigCache config;
        public final SystemHookSetup systemHooks;

        public NetworkMngrInit(KConfigCore config, SystemHookSetup systemHooks) {
            this.config = new KConfigCache(config);
            this.systemHooks = systemHooks;
        }
    }
}
