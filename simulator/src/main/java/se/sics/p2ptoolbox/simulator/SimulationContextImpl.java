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
package se.sics.p2ptoolbox.simulator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import se.sics.gvod.net.VodAddress;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.p2ptoolbox.simulator.cmd.OperationCmd;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimulationContextImpl implements SimulationContext {

    private OperationCmd.ValidationException failureCause = null;
    
    private final Random rand;
    private final VodAddress simulatorAddress;

    private final Map<Integer, Map<Class<? extends PortType>, Port>> ports;
    private final Map<String, Object> otherContext;

    public SimulationContextImpl(Random rand, VodAddress simulatorAddress) {
        this.rand = rand;
        this.simulatorAddress = simulatorAddress;
        this.ports = new HashMap<Integer, Map<Class<? extends PortType>, Port>>();
        this.otherContext = new HashMap<String, Object>();
    }

    public boolean registerNode(Integer nodeId) {
        if (ports.containsKey(nodeId)) {
            return false;
        }
        ports.put(nodeId, new HashMap<Class<? extends PortType>, Port>());
        return true;
    }

    public boolean isNodeRegistered(Integer nodeId) {
        return ports.containsKey(nodeId);
    }

    public boolean registerPort(Integer nodeId, Class<? extends PortType> portType, Port port) {
        Map<Class<? extends PortType>, Port> localPorts = ports.get(nodeId);
        if (localPorts == null) {
            return false;
        }
        if (localPorts.containsKey(portType)) {
            return false;
        }
        localPorts.put(portType, port);
        return true;
    }

    public Port getPort(Integer nodeId, Class<? extends PortType> portType) {
        Map<Class<? extends PortType>, Port> localPorts = ports.get(nodeId);
        if (localPorts == null) {
            return null;
        }
        return localPorts.get(portType);
    }

    public VodAddress getSimulatorAddress() {
        return simulatorAddress;
    }
    
    public boolean canContinue() {
        return failureCause == null ;
    }
    
    public void fail(OperationCmd.ValidationException cause) {
        this.failureCause = cause;
    }

    @Override
    public Random getRand() {
        return rand;
    }

    /**
     * @param identifier
     * @param obj
     * @return false if registration could not happen. Possible causes: 1. there
     * is already an object registered with that identifier
     */
    @Override
    public boolean register(String identifier, Object obj) {
        if (otherContext.containsKey(identifier)) {
            return false;
        }
        otherContext.put(identifier, obj);
        return true;
    }

    @Override
    public Object get(String identifier) {
        return otherContext.get(identifier);
    }

    public OperationCmd.ValidationException getSimulationResult() {
        return failureCause;
    }
}
