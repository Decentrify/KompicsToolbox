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
package se.sics.ktoolbox.simulator.instrumentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import se.sics.ktoolbox.simulator.SimulationScenario;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class InstrumentationHelper {

    public static void store(SimulationScenario scenario) {
        try {
            File file = File.createTempFile("scenario", ".bin");
            System.setProperty("scenario", file.getAbsolutePath());
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(scenario);
                oos.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SimulationScenario load(String scenarioFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(scenarioFile))) {
            SimulationScenario scenario = (SimulationScenario) ois.readObject();
            return scenario;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
