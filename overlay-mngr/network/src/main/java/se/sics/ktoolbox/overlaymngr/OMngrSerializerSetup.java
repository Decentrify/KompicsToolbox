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
package se.sics.ktoolbox.overlaymngr;

import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.gradient.GradientSerializerSetup;
import se.sics.ktoolbox.overlaymngr.util.ServiceView;
import se.sics.ktoolbox.overlaymngr.util.ServiceViewSerializer;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OMngrSerializerSetup {

    public static int serializerIds = 1;

    public static enum OMngrSerializers {

        ServiceView(ServiceView.class, "serviceViewSerializer");

        public final Class serializedClass;
        public final String serializerName;

        private OMngrSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static boolean checkSetup() {
        for (OMngrSerializers cs : OMngrSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                return false;
            }
        }
        return BasicSerializerSetup.checkSetup()
                && CroupierSerializerSetup.checkSetup()
                && GradientSerializerSetup.checkSetup();
    }

    public static int registerSerializers(int startingId) {
        int currentId = startingId;

        ServiceViewSerializer serviceViewSerializer = new ServiceViewSerializer(currentId++);
        Serializers.register(serviceViewSerializer, OMngrSerializers.ServiceView.serializerName);
        Serializers.register(OMngrSerializers.ServiceView.serializedClass, OMngrSerializers.ServiceView.serializerName);

        Assert.assertEquals(serializerIds, currentId - startingId);
        return currentId;
    }
}
