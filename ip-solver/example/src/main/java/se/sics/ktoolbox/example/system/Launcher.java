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
package se.sics.ktoolbox.example.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ktoolbox.ipsolver.util.IpAddressStatus;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    private Positive<IpSolverPort> ipSolver;
    private List<EnumSet<GetIp.NetworkInterfacesMask>> examples = new ArrayList<EnumSet<GetIp.NetworkInterfacesMask>>();
    {
        examples.add(EnumSet.of(GetIp.NetworkInterfacesMask.ALL));
        examples.add(EnumSet.of(GetIp.NetworkInterfacesMask.PUBLIC)); 
        examples.add(EnumSet.of(GetIp.NetworkInterfacesMask.PRIVATE));
        examples.add(EnumSet.of(GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE));
        examples.add(EnumSet.of(GetIp.NetworkInterfacesMask.PUBLIC, GetIp.NetworkInterfacesMask.PRIVATE));
    }
    public Launcher() {
        LOG.info("initiating...");

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        
        System.setProperty("java.net.preferIPv4Stack", "true");
        Component ipSolverComp = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
        ipSolver = ipSolverComp.getPositive(IpSolverPort.class);
        subscribe(handleGetIp, ipSolver);
    }

    public Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("starting...");
            
            trigger(new GetIp.Req(examples.get(0)), ipSolver);
        }
    };

    public Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("stopping...");
        }
    };
    
    public Handler handleGetIp = new Handler<GetIp.Resp>() {
        @Override
        public void handle(GetIp.Resp resp) {
            LOG.info("received GetIp response for netInterfaces:{}", examples.get(0));
            for(IpAddressStatus addr : resp.addrs) {
                LOG.info("{}", addr);
            }
            examples.remove(0);
            if(!examples.isEmpty()) {
                trigger(new GetIp.Req(examples.get(0)), ipSolver);
            }
        }
    };
}
