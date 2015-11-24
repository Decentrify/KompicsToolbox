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

package se.sics.ktoolbox.util.proxy.example.deploy;

import se.sics.ktoolbox.util.address.resolution.AddressResolutionHelper;
import se.sics.ktoolbox.util.proxy.SystemHookSetup;
import se.sics.ktoolbox.util.proxy.example.core.ExampleComp;
import se.sics.ktoolbox.util.proxy.example.hooks.XYHookDefinition;
import se.sics.ktoolbox.util.proxy.example.serializer.ExampleSerializerSetup;
import se.sics.ktoolbox.util.proxy.network.NetworkHookFactory;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SystemSetup {
    public static void setup(SystemHookSetup systemHooks) {
        systemHooks.register(ExampleComp.RequiredHooks.XY_HOOK.name(), new XYHookDefinition());
        systemHooks.register(ExampleComp.RequiredHooks.NETWORK_HOOK.name(), NetworkHookFactory.getNettyNetwork());
        
        AddressResolutionHelper.reset();
        AddressResolutionHelper.useBasicAddresses();
        
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = ExampleSerializerSetup.registerSerializers(serializerId);
    }
}
