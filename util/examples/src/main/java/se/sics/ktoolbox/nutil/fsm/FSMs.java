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
package se.sics.ktoolbox.nutil.fsm;

import se.sics.ktoolbox.nutil.fsm.ids.FSMIdRegistry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMs {
  public static final String fsm1 = "fsm1";
  private static final byte bfsm1 = (byte)1;
  public static final String fsm2 = "fsm2";
  private static final byte bfsm2 = (byte)2;
  
  public static void registerFSMs() {
    FSMIdRegistry.registerPrefix(fsm1, bfsm1);
    FSMIdRegistry.registerPrefix(fsm2, bfsm2);
  }
}
