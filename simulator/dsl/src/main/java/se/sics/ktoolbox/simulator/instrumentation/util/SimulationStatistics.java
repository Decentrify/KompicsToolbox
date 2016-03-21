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
package se.sics.ktoolbox.simulator.instrumentation.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.identifiable.UUIDIdentifiable;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public enum SimulationStatistics {
    
    INSTANCE; //singleton

    private static final Logger LOG = LoggerFactory.getLogger("SimulationStatistics");

    private static final String statisticsDir = "./src/test/resources/";
    private static final String seedsFile = statisticsDir + "seeds";
    private static final String eventsFile = statisticsDir + "events";
    
    private Long windowTime;
    private List<Triplet<ComponentDefinition, Object, KompicsEvent>> executionWindow;
    private List<Pair<String, Long>> seeds;

    private SimulationStatistics() {
        windowTime = 0l;
        executionWindow = new ArrayList<>();
    }

    public static void cleanStatistics() {
        try {
            File eventsF = new File(eventsFile);
            if (eventsF.exists()) {
                SecureRandom sr = new SecureRandom();
                String renamedStatisticsFile = statisticsDir + "events-" + sr.nextInt();
                eventsF.renameTo(new File(renamedStatisticsFile));
            }
            eventsF.createNewFile();
            
            File seedF = new File(seedsFile);
            if (eventsF.exists()) {
                SecureRandom sr = new SecureRandom();
                String renamedStatisticsFile = statisticsDir + "seeds-" + sr.nextInt();
                eventsF.renameTo(new File(renamedStatisticsFile));
            }
            eventsF.createNewFile();
        } catch (IOException ex) {
            LOG.error("error creating simulation statistics file");
            throw new RuntimeException(ex);
        }
    }

    public void executeEvent(ComponentDefinition comp, Object handler, KompicsEvent event) {
        Long now = System.currentTimeMillis();
        if (!windowTime.equals(now)) {
            dumpEvents();
            executionWindow = new ArrayList<>();
            windowTime = now;
        }
        executionWindow.add(Triplet.with(comp, handler, event));
    }

    private void dumpEvents() {
        try (Writer writer = new BufferedWriter(new FileWriter(eventsFile, true))) {
            for (Triplet<ComponentDefinition, Object, KompicsEvent> event : executionWindow) {
                StringBuilder sb = new StringBuilder(windowTime.toString());
                sb.append(" ").append(event.getValue0().getClass().getName());
                sb.append(" ").append(event.getValue1().getClass().getName());
                sb.append(" ").append(event.getValue2().getClass().getName());
//                if (event.getValue2() instanceof UUIDIdentifiable) {
//                    UUIDIdentifiable identifiableEvent = (UUIDIdentifiable) event.getValue2();
//                    sb.append("<").append(identifiableEvent.getId()).append(">");
//                }
                sb.append("\n");
                writer.append(sb.toString());
            }
        } catch (IOException ex) {
            LOG.error("error writting to file");
            throw new RuntimeException(ex);
        }
    }
    
    public void registerSeed(String callingClass, Long seed) {
        seeds.add(Pair.with(callingClass, seed));
    }
    
    private void dumpSeeds() {
        try (Writer writer = new BufferedWriter(new FileWriter(seedsFile, true))) {
            for (Pair<String, Long> event : seeds) {
                StringBuilder sb = new StringBuilder();
                sb.append(event.getValue0());
                sb.append(" ").append(event.getValue1().toString()).append("\n");
                System.err.println(sb.toString());
                writer.append(sb.toString());
            }
        } catch (IOException ex) {
            LOG.error("error writting to file");
            throw new RuntimeException(ex);
        }
    }
    
    public void terminateSimulation() {
        dumpEvents();
        dumpSeeds();
    }
}
