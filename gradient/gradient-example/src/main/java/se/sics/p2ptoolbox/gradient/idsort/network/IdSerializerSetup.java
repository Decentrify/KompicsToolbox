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
package se.sics.p2ptoolbox.gradient.idsort.network;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.croupier.CroupierSerializerSetup;
import se.sics.p2ptoolbox.gradient.GradientSerializerSetup;
import se.sics.p2ptoolbox.gradient.idsort.IdView;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class IdSerializerSetup {

    public static int serializerIds = 1;

    public static enum IdSerializers {

        IdView(IdView.class, "idViewSerializer");

        public final Class serializedClass;
        public final String serializerName;

        private IdSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static void oneTimeSetup() {
        int currentId = 0;
        BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId += BasicSerializerSetup.serializerIds;
        
        currentId = CroupierSerializerSetup.registerSerializers(currentId);
        currentId = GradientSerializerSetup.registerSerializers(currentId);
        currentId = registerSerializers(currentId);
    }
    
    private static int registerSerializers(int startingId) {
        int currentId = startingId;

        IdViewSerializer idSerializer = new IdViewSerializer(currentId++);
        Serializers.register(idSerializer, IdSerializers.IdView.serializerName);
        Serializers.register(IdSerializers.IdView.serializedClass, IdSerializers.IdView.serializerName);
        
        return currentId;
    }
}
