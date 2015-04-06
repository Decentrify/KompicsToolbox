/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.croupier.croupier.example;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.Test;
import se.sics.kompics.Kompics;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.p2ptoolbox.croupier.example.simulation.CroupierSimulationResult;
import se.sics.p2ptoolbox.croupier.example.simulation.CroupierSimulationScenarios;
import se.sics.p2ptoolbox.simulator.run.LauncherComp;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicNatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierSimulationTest {

    @Test
    public void simpleBootTest() {
        simpleBoot(20, 1234, 100);
    }

    private void simpleBoot(int nodes, long seed, int simulatedSeconds) {
        LauncherComp.scheduler = new SimulatorScheduler();
        LauncherComp.scenario = CroupierSimulationScenarios.simpleBoot(nodes, 1234L, simulatedSeconds);
        //
        try {
            LauncherComp.simulatorClientAddress = new BasicNatedAddress(new BasicAddress(InetAddress.getByName("127.0.0.1"), 30000, -1));
        } catch (UnknownHostException ex) {
            throw new RuntimeException("cannot create address for localhost");
        }

        Kompics.setScheduler(LauncherComp.scheduler);
        Kompics.createAndStart(LauncherComp.class, 1);
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        Assert.assertEquals(null, CroupierSimulationResult.failureCause);
    }
}
