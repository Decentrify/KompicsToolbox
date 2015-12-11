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

package se.sics.ktoolbox.util.proxy.example.deploy;

import com.typesafe.config.ConfigFactory;
import se.sics.kompics.Kompics;
import se.sics.ktoolbox.util.proxy.SystemHookSetup;
import se.sics.ktoolbox.util.proxy.example.core.ExampleComp;
import se.sics.ktoolbox.util.config.KConfigCore;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExampleLauncher {
    
    public static void main(String[] args) {
        KConfigCore configCore = new KConfigCore(ConfigFactory.load());
        SystemHookSetup systemHooks = new SystemHookSetup();
        SystemSetup.setup(systemHooks);
        
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(ExampleComp.class, new ExampleComp.ExampleInit(configCore, systemHooks),
                Runtime.getRuntime().availableProcessors());
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
