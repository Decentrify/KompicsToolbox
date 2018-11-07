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
package se.sics.ktoolbox.gradient;

import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.gradient.msg.GradientShuffle;
import se.sics.ktoolbox.gradient.msg.GradientShuffleSerializer;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.gradient.util.GradientContainerSerializer;
import se.sics.ktoolbox.gradient.util.GradientLocalView;
import se.sics.ktoolbox.gradient.util.GradientLocalViewSerializer;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;


/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientSerializerSetup {

    public static int serializerIds = 4;

    public static enum GradientSerializers {

        GradientContainer(GradientContainer.class, "gradientContainerSerializer"),
        GradientLocalView(GradientLocalView.class, "gradientLocalViewSerializer"),
        GradientShuffleRequest(GradientShuffle.Request.class, "gradientShuffleRequestSerializer"),
        GradientShuffleResponse(GradientShuffle.Response.class, "gradientShuffleResponseSerializer");

        public final Class serializedClass;
        public final String serializerName;

        private GradientSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static boolean checkSetup() {
        for (GradientSerializers gs : GradientSerializers.values()) {
            if (Serializers.lookupSerializer(gs.serializedClass) == null) {
                return false;
            }
        }
        return BasicSerializerSetup.checkSetup();
    }

    public static int registerSerializers(int startingId) {
        int currentId = startingId;

        GradientContainerSerializer gradientContainerSerializer = new GradientContainerSerializer(currentId++);
        Serializers.register(gradientContainerSerializer, GradientSerializers.GradientContainer.serializerName);
        Serializers.register(GradientSerializers.GradientContainer.serializedClass, GradientSerializers.GradientContainer.serializerName);
        
        GradientLocalViewSerializer gradientLocalViewSerializer = new GradientLocalViewSerializer(currentId++);
        Serializers.register(gradientLocalViewSerializer, GradientSerializers.GradientLocalView.serializerName);
        Serializers.register(GradientSerializers.GradientLocalView.serializedClass, GradientSerializers.GradientLocalView.serializerName);

        Class msgIdType = IdentifierRegistryV2.idType(BasicIdentifiers.Values.MSG);
        GradientShuffleSerializer.Request gradientShuffleRequestSerializer = new GradientShuffleSerializer.Request(currentId++, msgIdType);
        Serializers.register(gradientShuffleRequestSerializer, GradientSerializers.GradientShuffleRequest.serializerName);
        Serializers.register(GradientSerializers.GradientShuffleRequest.serializedClass, GradientSerializers.GradientShuffleRequest.serializerName);

        GradientShuffleSerializer.Response gradientShuffleResponseSerializer = new GradientShuffleSerializer.Response(currentId++, msgIdType);
        Serializers.register(gradientShuffleResponseSerializer, GradientSerializers.GradientShuffleResponse.serializerName);
        Serializers.register(GradientSerializers.GradientShuffleResponse.serializedClass, GradientSerializers.GradientShuffleResponse.serializerName);

        Assert.assertEquals(serializerIds, currentId - startingId);
        return currentId;
    }
}
