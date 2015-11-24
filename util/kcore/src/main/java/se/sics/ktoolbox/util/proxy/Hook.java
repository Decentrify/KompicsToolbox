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
package se.sics.ktoolbox.util.proxy;

import se.sics.kompics.ComponentProxy;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Hook {

    public static interface Definition<P extends Parent, SI extends SetupInit, SR extends SetupResult, S extends StartInit, T extends TearInit> {

        public SR setup(ComponentProxy proxy, P parent, SI hookInit);

        public void start(ComponentProxy proxy, P parent, SR setup, S startInit);
        
        public void tearDown(ComponentProxy proxy, P parent, SR setup, T tearInit);
    }

    public static interface SetupInit {
    }

    public static interface SetupResult {
    }

    public static abstract class StartInit {
        public final boolean started;
        
        public StartInit(boolean started) {
            this.started = started;
        }
    }

    public static abstract class TearInit {
        public final boolean killed;
        
        public TearInit(boolean killed) {
            this.killed = killed;
        }
    }
    
    public static interface Parent {
    }
    
    public static interface Required {
    }
}
