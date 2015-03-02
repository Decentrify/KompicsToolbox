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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.kompics.Kompics;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.p2ptoolbox.simulator.example.proj.MyStatusMsg;
import se.sics.p2ptoolbox.simulator.example.simulator.MyExperimentResult;
import se.sics.p2ptoolbox.simulator.example.simulator.ScenarioGen;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
@RunWith(JUnit4.class)
public class MyTest {
    public static long seed = 123;
    
    @Test
    public void simpleBoot() {
        LauncherComp.scheduler = new SimulatorScheduler();
        LauncherComp.scenario = ScenarioGen.simpleBoot(seed);
        LauncherComp.systemStatusHandlers.add(new SystemStatusHandler() {

            public Class getStatusMsgType() {
                return MyStatusMsg.Status1.class;
            }

            public void handle(KompicsEvent msg, SimulationContext context) {
                System.out.println("handling status1");
            }
        });
        
        LauncherComp.systemStatusHandlers.add(new SystemStatusHandler() {

            public Class getStatusMsgType() {
                return MyStatusMsg.Status2.class;
            }

            public void handle(KompicsEvent msg, SimulationContext context) {
                System.out.println("handling status2");
            }
        });
        
        Kompics.setScheduler(LauncherComp.scheduler);
        Kompics.createAndStart(LauncherComp.class, 1);
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        
        Assert.assertEquals(null, MyExperimentResult.failureCause);
    }
    
    @Test
    public void simpleNetworkModelChange() {
        LauncherComp.scheduler = new SimulatorScheduler();
        LauncherComp.scenario = ScenarioGen.simpleChangeNetworkModel(seed);
        Kompics.setScheduler(LauncherComp.scheduler);
        Kompics.createAndStart(LauncherComp.class, 1);
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

}
