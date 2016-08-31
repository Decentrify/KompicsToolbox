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

package se.sics.ledbat.system;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ledbat.LedbatSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ExSerializerSetup {
    public static int serializerIds = 2;
    
    public static enum ExSerializers {
        ExMsgReq(ExMsg.Request.class, "exMsgReq"),
        ExMsgResp(ExMsg.Response.class, "exMsgResp");
        public final Class serializedClass;
        public final String serializerName;

        private ExSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static boolean checkSetup() {
        for (ExSerializers gs : ExSerializers.values()) {
            if (Serializers.lookupSerializer(gs.serializedClass) == null) {
                return false;
            }
        }
        if(!BasicSerializerSetup.checkSetup()) {
            return false;
        }
        if(!LedbatSerializerSetup.checkSetup()) {
            return false;
        }
        return true;
    }
    
    public static int registerSerializers(int startingId) {
        int currentId = startingId;
        
        ExMsgSerializer.Request exMsgReqSerializer = new ExMsgSerializer.Request(currentId++);
        Serializers.register(exMsgReqSerializer, ExSerializers.ExMsgReq.serializerName);
        Serializers.register(ExSerializers.ExMsgReq.serializedClass, ExSerializers.ExMsgReq.serializerName);
        
        ExMsgSerializer.Response exMsgRespSerializer = new ExMsgSerializer.Response(currentId++);
        Serializers.register(exMsgRespSerializer, ExSerializers.ExMsgResp.serializerName);
        Serializers.register(ExSerializers.ExMsgResp.serializedClass, ExSerializers.ExMsgResp.serializerName);
        
        assert startingId + serializerIds == currentId;
        return currentId;
    }
}
