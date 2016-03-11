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
package se.sics.ktoolbox.netmngr.ipsolver;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.ktoolbox.netmngr.ipsolver.util.IpAddressStatus;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpSolverComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(IpSolverComp.class);
    private Negative<IpSolverPort> ipSolver = provides(IpSolverPort.class);

    private final String logPrefix = "";

    public IpSolverComp(IpSolverInit init) {
        LOG.info("{}initiating...", logPrefix);
        if (System.getProperty("java.net.preferIPv4Stack") == null 
                || !System.getProperty("java.net.preferIPv4Stack").equals("true")) {
            LOG.error("{}java.net.preferIPv4Stack not set", logPrefix);
            throw new RuntimeException("java.net.preferIPv4Stack not set");
        }

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleGetIp, ipSolver);
    }

    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    private Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop e) {
            LOG.info("{}stopping...", logPrefix);
        }
    };

    private Handler handleGetIp = new Handler<IpSolve.Request>() {
        @Override
        public void handle(IpSolve.Request req) {
            LOG.debug("{}GetIp request", logPrefix);
            try {
                Set<IpAddressStatus> niSet = IpSolver.getLocalNetworkInterfaces(req.netInterfaces);
                //TODO Alex - sort the addresses based on the IpComparator?
                ArrayList<IpAddressStatus> addressList = new ArrayList<IpAddressStatus>(niSet);
                InetAddress boundIp = InetAddress.getLocalHost();
                LOG.debug("{}GetIp responding", logPrefix);
                answer(req, req.answer(addressList, boundIp));
            } catch (SocketException ex) {
                LOG.error("socket error while scanning network interfaces");
                throw new RuntimeException("socket error while scanning network interfaces", ex);
            } catch (UnknownHostException ex) {
                LOG.error("host error while scanning network interfaces");
                throw new RuntimeException("host error while scanning network interfaces", ex);
            }
        }
    };
    
    //TODO Alex - eventually you might want to do periodic recheck of interfaces
//    private UUID stateCheckTid = null;
//    private EnumSet<GetIp.NetworkInterfacesMask> filters;
//    private InetAddress boundIp;
//    private Set<IpAddressStatus> niSet = new HashSet<IpAddressStatus>();
//    private void recheckInterfaces() {
//        LOG.debug("rechecking network interfaces");
//        try {
//            Set<IpAddressStatus> newNISet = IpHelper.getLocalNetworkInterfaces(filters);
//            Set<IpAddressStatus> difference = Sets.symmetricDifference(niSet, newNISet);
//            InetAddress newBoundIp = InetAddress.getLocalHost();
//            if (!difference.isEmpty()) {
//                niSet = newNISet;
//                LOG.info("network interface change");
//                trigger(new IpChange(new ArrayList<IpAddressStatus>(niSet), boundIp), ipSolver);
//            }
//            if (!newBoundIp.equals(boundIp)) {
//                boundIp = newBoundIp;
//                LOG.info("boundIp change");
//                trigger(new IpChange(new ArrayList<IpAddressStatus>(niSet), boundIp), ipSolver);
//            }
//        } catch (SocketException ex) {
//            LOG.error("socket error while rescanning network interfaces");
//            throw new RuntimeException("socket error while rescanning network interfaces", ex);
//        } catch (UnknownHostException ex) {
//            LOG.error("host error while rescanning network interfaces");
//            throw new RuntimeException("host error while rescanning network interfaces", ex);
//        }
//    }
    
        //TODO Alex - if you do any periodic rescaning.. might want to do state check for memory leaks
//    private void schedulePeriodicStateCheck() {
//        if (stateCheckTid != null) {
//            LOG.warn("double starting periodic state check");
//            return;
//        }
//        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(IpSolverConfig.RECHECK_NETWORK_INTERFACES_PERIOD, IpSolverConfig.RECHECK_NETWORK_INTERFACES_PERIOD);
//        StateCheckTimeout timeout = new StateCheckTimeout(spt);
//        spt.setTimeoutEvent(timeout);
//        stateCheckTid = timeout.getTimeoutId();
//        trigger(spt, timer);
//    }
//
//    private void cancelPeriodicStateCheck() {
//        if (stateCheckTid == null) {
//            LOG.warn("double stopping periodic shuffle");
//            return;
//        }
//        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(stateCheckTid);
//        stateCheckTid = null;
//        trigger(cpt, timer);
//    }
//
//    private Handler<StateCheckTimeout> handleStateCheck = new Handler<StateCheckTimeout>() {
//        @Override
//        public void handle(StateCheckTimeout event) {
//        }
//    };
//    
//    public static class StateCheckTimeout extends Timeout {
//
//        public StateCheckTimeout(SchedulePeriodicTimeout request) {
//            super(request);
//        }
//
//        @Override
//        public String toString() {
//            return "State Check Timeout";
//        }
//    }

    public static class IpSolverConfig {

        public static final int RECHECK_NETWORK_INTERFACES_PERIOD = 30 * 1000;
        public static final int DISCOVERY_TIMEOUT = 2 * 1000;
        public static final int LEASE_DURATION = 60 * 60 * 1000;
    }

    public static class IpSolverInit extends Init<IpSolverComp> {
    }
}
