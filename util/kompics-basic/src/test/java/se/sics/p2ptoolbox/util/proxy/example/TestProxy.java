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

package se.sics.p2ptoolbox.util.proxy.example;

import org.junit.Test;
import se.sics.kompics.Kompics;
import se.sics.p2ptoolbox.util.proxy.example.core.HookParentComp;
import se.sics.p2ptoolbox.util.proxy.example.system.SystemSetup;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestProxy {
    @Test
    public void test() {
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(HookParentComp.class, new HookParentComp.HookParentInit(SystemSetup.hookSetup()),
                Runtime.getRuntime().availableProcessors());
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
