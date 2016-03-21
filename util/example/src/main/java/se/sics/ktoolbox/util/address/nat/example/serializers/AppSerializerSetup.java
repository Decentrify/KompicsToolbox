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
package se.sics.ktoolbox.util.address.nat.example.serializers;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.address.nat.example.msg.AppMsg;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AppSerializerSetup {

    public static int serializerIds = 2;

    public static enum AppSerializers {

        AppPingSerializer(AppMsg.Ping.class, "appPingSerializer"),
        AppPongSerializer(AppMsg.Pong.class, "appPongSerializer");

        public final Class serializedClass;
        public final String serializerName;

        private AppSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static void checkSetup() {
        for (AppSerializers cs : AppSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + cs.serializedClass);
            }
        }
        BasicSerializerSetup.checkSetup();
    }

    public static int registerSerializers(int startingId) {
        int currentId = startingId;

        AppMsgSerializer.Ping appPingSerializer = new AppMsgSerializer.Ping(currentId++);
        Serializers.register(appPingSerializer, AppSerializers.AppPingSerializer.serializerName);
        Serializers.register(AppSerializers.AppPingSerializer.serializedClass, AppSerializers.AppPingSerializer.serializerName);

        AppMsgSerializer.Pong appPongSerializer = new AppMsgSerializer.Pong(currentId++);
        Serializers.register(appPongSerializer, AppSerializers.AppPongSerializer.serializerName);
        Serializers.register(AppSerializers.AppPongSerializer.serializedClass, AppSerializers.AppPongSerializer.serializerName);

        assert serializerIds == currentId - startingId;
        return currentId;
    }
}
