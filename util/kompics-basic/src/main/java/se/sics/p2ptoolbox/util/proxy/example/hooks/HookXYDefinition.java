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
package se.sics.p2ptoolbox.util.proxy.example.hooks;

import se.sics.kompics.Component;
import se.sics.kompics.Start;
import se.sics.p2ptoolbox.util.proxy.ComponentProxy;
import se.sics.p2ptoolbox.util.proxy.example.core.ExampleComp.ExampleHookParent;
import se.sics.p2ptoolbox.util.proxy.example.core.HookXY;
import se.sics.p2ptoolbox.util.proxy.example.core.PortX;
import se.sics.p2ptoolbox.util.proxy.example.core.PortY;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HookXYDefinition implements HookXY.Definition {

    @Override
    public HookXY.SetupResult setup(ComponentProxy proxy, ExampleHookParent hookParent, HookXY.SetupInit setupInit) {
        Component[] comp = new Component[2];
        comp[0] = proxy.create(ComponentB.class, new ComponentB.InitB());
        comp[1] = proxy.create(ComponentA.class, new ComponentA.InitA(setupInit.field1));
        proxy.connect(comp[1].getNegative(PortZ.class), comp[0].getPositive(PortZ.class));
        return new HookXY.SetupResult(comp[1].getPositive(PortX.class), comp[0].getNegative(PortY.class), comp);
    }

    @Override
    public void start(ComponentProxy proxy, ExampleHookParent hookParent, HookXY.SetupResult setupResult, HookXY.StartInit startInit) {
        if (!startInit.started) {
            proxy.trigger(Start.event, setupResult.components[0].control());
            proxy.trigger(Start.event, setupResult.components[1].control());
        }
    }

    @Override
    public void preStop(ComponentProxy proxy, ExampleHookParent hookParent, HookXY.SetupResult setupResult, HookXY.TearInit tear) {
    }
}
