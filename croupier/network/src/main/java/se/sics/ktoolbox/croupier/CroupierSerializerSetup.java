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

package se.sics.ktoolbox.croupier;

import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.croupier.msg.CroupierShuffle;
import se.sics.ktoolbox.croupier.msg.CroupierShuffleSerializer;
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.ktoolbox.croupier.util.CroupierContainerSerializer;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierSerializerSetup {
    public static int serializerIds = 3;
    
    public static enum CroupierSerializers {
        CroupierContainer(CroupierContainer.class, "croupierContainerSerializer"),
        CroupierShuffleRequest(CroupierShuffle.Request.class, "croupierShuffleRequestSerializer"),
        CroupierShuffleResponse(CroupierShuffle.Response.class, "croupierShuffleResponseSerializer");
        
        public final Class serializedClass;
        public final String serializerName;

        private CroupierSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static boolean checkSetup() {
        for (CroupierSerializers cs : CroupierSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
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
        
        CroupierContainerSerializer croupierContainerSerializer = new CroupierContainerSerializer(currentId++);
        Serializers.register(croupierContainerSerializer, CroupierSerializers.CroupierContainer.serializerName);
        Serializers.register(CroupierSerializers.CroupierContainer.serializedClass, 
          CroupierSerializers.CroupierContainer.serializerName);
        
        Class msgIdType = IdentifierRegistryV2.idType(BasicIdentifiers.Values.MSG);
        CroupierShuffleSerializer.Request croupierShuffleRequestSerializer
          = new CroupierShuffleSerializer.Request(currentId++);
        Serializers.register(croupierShuffleRequestSerializer, 
          CroupierSerializers.CroupierShuffleRequest.serializerName);
        Serializers.register(CroupierSerializers.CroupierShuffleRequest.serializedClass, 
          CroupierSerializers.CroupierShuffleRequest.serializerName);
        
        CroupierShuffleSerializer.Response croupierShuffleResponseSerializer 
          = new CroupierShuffleSerializer.Response(currentId++);
        Serializers.register(croupierShuffleResponseSerializer, 
          CroupierSerializers.CroupierShuffleResponse.serializerName);
        Serializers.register(CroupierSerializers.CroupierShuffleResponse.serializedClass, 
          CroupierSerializers.CroupierShuffleResponse.serializerName);
        
        Assert.assertEquals(serializerIds, currentId - startingId);
        return currentId;
    }
}
