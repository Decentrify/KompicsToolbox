/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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

package se.sics.p2ptoolbox.croupier.core;

import se.sics.p2ptoolbox.serialization.SerializationContext;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierSetup {
    public static void oneTimeSetup(SerializationContext context) {
        if(CroupierNetworkSettings.hasContext()) {
            throw new RuntimeException("croupier has already been setup - do not call this multiple times(for each croupier instance)");
        }
        CroupierNetworkSettings.setContext(context);
        if(!CroupierNetworkSettings.checkPreCond()) {
            throw new RuntimeException("context incomplete for croupier");
        }
        CroupierNetworkSettings.registerSerializers();
    }
}
