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
package se.sics.ktoolbox.util.proxy.example.hooks;

import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Kill;
import se.sics.kompics.Start;
import se.sics.kompics.UniDirectionalChannel;
import se.sics.ktoolbox.util.proxy.example.core.XYHook;
import se.sics.ktoolbox.util.proxy.example.core.PortX;
import se.sics.ktoolbox.util.proxy.example.core.PortY;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class XYHookDefinition implements XYHook.Definition {

    @Override
    public XYHook.SetupResult setup(ComponentProxy proxy, XYHook.Parent hookParent, XYHook.SetupInit setupInit) {
        Component[] components = new Component[2];
        Channel[][] channels = new Channel[0][];
        components[0] = proxy.create(ComponentB.class, new ComponentB.InitB());
        components[1] = proxy.create(ComponentA.class, new ComponentA.InitA(setupInit.field1));
        proxy.connect(components[0].getPositive(PortX.class), components[1].getNegative(PortX.class), UniDirectionalChannel.TWO_WAY);
        return new XYHook.SetupResult(components[1].getPositive(PortX.class), components[0].getNegative(PortY.class), components, channels);
    }

    @Override
    public void start(ComponentProxy proxy, XYHook.Parent hookParent, XYHook.SetupResult setupResult, XYHook.StartInit startInit) {
        if (!startInit.started) {
            proxy.trigger(Start.event, setupResult.components[0].control());
            proxy.trigger(Start.event, setupResult.components[1].control());
        }
        hookParent.on();
    }

    @Override
    public void tearDown(ComponentProxy proxy, XYHook.Parent hookParent, XYHook.SetupResult setupResult, XYHook.TearInit tearInit) {
        if(!tearInit.killed) {
            proxy.trigger(Kill.event, setupResult.components[0].control());
            proxy.trigger(Kill.event, setupResult.components[1].control());
        }
    }
}
