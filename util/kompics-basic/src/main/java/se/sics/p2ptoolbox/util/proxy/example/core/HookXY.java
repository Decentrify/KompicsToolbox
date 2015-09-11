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
package se.sics.p2ptoolbox.util.proxy.example.core;

import se.sics.kompics.Component;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.p2ptoolbox.util.proxy.Hook;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HookXY {
    public static interface Definition extends Hook.Definition<Init, InitResult, Tear> {
    }
 
    public static class Init implements Hook.Init {

        public final boolean field1;

        public Init(boolean field1) {
            this.field1 = field1;
        }
    }
    
    public static class InitResult implements Hook.InitResult {
        public final Positive<PortX> portX;
        public final Negative<PortY> portY;
        public final Component[] components;
        
        public InitResult(Positive<PortX> portX, Negative<PortY> portY, Component[] components) {
            this.portX = portX;
            this.portY = portY;
            this.components = components;
        }
    }
    
    public static class Tear implements Hook.Tear {
        public final Component[] components;
        
        public Tear(Component[] components) {
            this.components = components;
        }
    }
}
