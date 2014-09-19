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
package se.sics.p2ptoolbox.croupier.core.net;

import java.util.HashMap;
import java.util.Map;
import se.sics.p2ptoolbox.croupier.api.CroupierMsg;
import se.sics.p2ptoolbox.croupier.api.CroupierSetup;
import se.sics.p2ptoolbox.croupier.api.net.PeerViewSerializer;
import se.sics.p2ptoolbox.croupier.core.msg.Shuffle;
import se.sics.p2ptoolbox.croupier.core.net.serializers.ShuffleAdapter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierContext {
    
    private final Map<Class<?>, Adapter<?>>
    
    public static final byte SHUFFLE_REQUEST = 0x01;
    public static final byte SHUFFLE_RESPONSE = 0x02;
    
    private static final Map<Byte, CroupierSerializer> croupierAdapters = new HashMap<Byte, CroupierSerializer>();
    {
        croupierAdapters.put(SHUFFLE_REQUEST, new ShuffleAdapter.Request());
        croupierAdapters.put(SHUFFLE_RESPONSE, new ShuffleAdapter.Response());
    }
    
    public static CroupierSerializer getAdapter(byte regCode) {
        return croupierAdapters.get(regCode);
    }

    public static <E extends CroupierMsg.Base> byte getRegCode(E msg) {
        if (msg instanceof Shuffle.Request) {
            return SHUFFLE_REQUEST;
        } else if (msg instanceof Shuffle.Response) {
            return SHUFFLE_RESPONSE;
        }
        throw new RuntimeException("no opcode translation");
    }

    public static <E extends CroupierMsg.Base> CroupierSerializer<E> getAdapter(E msg) {
        CroupierSerializer<E> adapter = getAdapter(getRegCode(msg));
        if (adapter == null) {
            throw new RuntimeException(new NullPointerException("unregistered adapter for msg" + msg.getClass()));
        }
        return adapter;
    }

    //***************************************************************************************************
    public final byte CROUPIER_NET_REQ;
    public final byte CROUPIER_NET_RESP;
    public final PeerViewSerializer pwAdapter;
    
    public CroupierContext(CroupierSetup setup) {
        this.CROUPIER_NET_REQ = setup.CROUPIER_NET_REQ;
        this.CROUPIER_NET_RESP = setup.CROUPIER_NET_RESP;
        this.pwAdapter = setup.pwAdapter;
    }
}
