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
package se.sics.ktoolbox.simulator.instrumentation.decorators.simple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SimpleSimulationStatistics {

    private static final Logger LOG = LoggerFactory.getLogger("SimulationStatistics");

    private static final String statisticsDir = "./src/test/resources/";
    private static final String statisticsFile = statisticsDir + "statistics.values";
    private Long windowTime;
    private List<Triplet<ComponentDefinition, Object, KompicsEvent>> executionWindow;

    public SimpleSimulationStatistics() {
        windowTime = 0l;
        executionWindow = new ArrayList<>();
    }

    public static void cleanStatistics() {
        try {
            File file = new File(statisticsFile);
            if (file.exists()) {
                SecureRandom sr = new SecureRandom();
                String renamedStatisticsFile = statisticsDir + "statistics-" + sr.nextInt() + ".values";
                file.renameTo(new File(renamedStatisticsFile));
            }
            file.createNewFile();
        } catch (IOException ex) {
            LOG.error("error creating simulation statistics file");
            throw new RuntimeException(ex);
        }
    }

    public void executeEvent(ComponentDefinition comp, Object handler, KompicsEvent event) {
        Long now = System.currentTimeMillis();
        if (!windowTime.equals(now)) {
            dump();
            executionWindow = new ArrayList<>();
            windowTime = now;
        }
        executionWindow.add(Triplet.with(comp, handler, event));
    }

    private void dump() {
        try (Writer writer = new BufferedWriter(new FileWriter(statisticsFile, true))) {
            for (Triplet<ComponentDefinition, Object, KompicsEvent> event : executionWindow) {
                StringBuilder sb = new StringBuilder(windowTime.toString());
                sb.append(" ").append(event.getValue0().getClass().getName());
                sb.append(" ").append(event.getValue1().getClass().getName());
                sb.append(" ").append(event.getValue2().getClass().getName()).append("\n");
                System.err.println(sb.toString());
                writer.append(sb.toString());
            }
        } catch (IOException ex) {
            LOG.error("error writting to file");
            throw new RuntimeException(ex);
        }
    }
}
