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

package se.sics.ledbat;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ledbat.ncore.msg.LedbatMsg;
import se.sics.ledbat.ncore.msg.LedbatMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class LedbatSerializerSetup {
    public static int serializerIds = 2;
    
    public static enum LedbatSerializers {
        LedbatMsgReq(LedbatMsg.Request.class, "ledbatMsgReq"),
        LedbatMsgResp(LedbatMsg.Response.class, "ledbatMsgResp");
        public final Class serializedClass;
        public final String serializerName;

        private LedbatSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static boolean checkSetup() {
        for (LedbatSerializers gs : LedbatSerializers.values()) {
            if (Serializers.lookupSerializer(gs.serializedClass) == null) {
                return false;
            }
        }
        if(!BasicSerializerSetup.checkSetup()) {
            return false;
        }
        return true;
    }
    
    public static int registerSerializers(int startingId) {
        int currentId = startingId;
        
        LedbatMsgSerializer.Request ledbatMsgReqSerializer = new LedbatMsgSerializer.Request(currentId++);
        Serializers.register(ledbatMsgReqSerializer, LedbatSerializers.LedbatMsgReq.serializerName);
        Serializers.register(LedbatSerializers.LedbatMsgReq.serializedClass, LedbatSerializers.LedbatMsgReq.serializerName);
        
        LedbatMsgSerializer.Response ledbatMsgRespSerializer = new LedbatMsgSerializer.Response(currentId++);
        Serializers.register(ledbatMsgRespSerializer, LedbatSerializers.LedbatMsgResp.serializerName);
        Serializers.register(LedbatSerializers.LedbatMsgResp.serializedClass, LedbatSerializers.LedbatMsgResp.serializerName);
        
        assert startingId + serializerIds == currentId;
        return currentId;
    }
}
