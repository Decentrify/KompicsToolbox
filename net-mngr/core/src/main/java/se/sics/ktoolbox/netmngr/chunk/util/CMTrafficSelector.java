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
package se.sics.ktoolbox.netmngr.chunk.util;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.netmngr.chunk.Chunk;
import se.sics.ktoolbox.netmngr.chunk.Chunkable;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.ports.TrafficSelector;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CMTrafficSelector {

    public static class Incoming extends TrafficSelector {

        @Override
        public boolean pass(KompicsEvent event) {
            if (event instanceof KContentMsg) {
                if (((KContentMsg) event).getContent() instanceof Chunkable) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class Outgoing extends TrafficSelector {

        @Override
        public boolean pass(KompicsEvent event) {
            if (event instanceof KContentMsg) {
                if (((KContentMsg) event).getContent() instanceof Chunk) {
                    return true;
                }
            }
            return false;
        }
    }
}
