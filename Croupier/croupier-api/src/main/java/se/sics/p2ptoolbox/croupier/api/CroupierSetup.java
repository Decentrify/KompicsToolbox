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

package se.sics.p2ptoolbox.croupier.api;

import static org.junit.Assert.*;
import se.sics.p2ptoolbox.croupier.api.net.CroupierContext;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierSetup {

    public static byte CROUPIER_NET_REQUEST = 0x00;
    public static byte CROUPIER_NET_RESPONSE = 0x00;
    
    public static void setupCroupierNetMsgs(byte croupierNetRequest, byte croupierNetResponse) {
        assertThat(
        if(CROUPIER_NET_REQUEST != 0x00 || CROUPIER_NET_RESPONSE !=0x00) {
            throw new RuntimeException("CroupierRegistry should only initialize net codes once");
        }
        CROUPIER_NET_REQUEST = croupierNetRequest;
        CROUPIER_NET_RESPONSE = croupierNetResponse;
    }
    
    public static void checkRegisteredCroupierNetMsgs() {
        if(CROUPIER_NET_REQUEST == 0x00 || CROUPIER_NET_RESPONSE ==0x00) {
            throw new RuntimeException("CroupierRegistry not properly initialized");
        }
    }
    
    private static CroupierContext croupierContexts = null;
//    public static void setupContext(CroupierContext) {
//        crou
//    }
}
