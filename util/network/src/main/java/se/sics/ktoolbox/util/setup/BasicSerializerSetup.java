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
package se.sics.ktoolbox.util.setup;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnIdsSerializer;
import se.sics.ktoolbox.nutil.conn.ConnMsgs;
import se.sics.ktoolbox.nutil.conn.ConnStatus;
import se.sics.ktoolbox.nutil.conn.ConnStatusSerializer;
import se.sics.ktoolbox.nutil.conn.ConnMsgsSerializer;
import se.sics.ktoolbox.nutil.conn.ConnState;
import se.sics.ktoolbox.nutil.conn.EmptyConnStateSerializer;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.basic.IntId;
import se.sics.ktoolbox.util.identifiable.basic.IntIdSerializer;
import se.sics.ktoolbox.util.identifiable.basic.SimpleByteId;
import se.sics.ktoolbox.util.identifiable.basic.SimpleByteIdSerializer;
import se.sics.ktoolbox.util.identifiable.basic.StringByteId;
import se.sics.ktoolbox.util.identifiable.basic.StringByteIdSerializer;
import se.sics.ktoolbox.util.identifiable.basic.UUIDId;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdSerializer;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayIdSerializer;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddressSerializer;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicContentMsgSerializer;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.BasicHeaderSerializer;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeaderSerializer;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImplSerializer;
import se.sics.ktoolbox.util.network.nat.NatType;
import se.sics.ktoolbox.util.network.nat.NatTypeSerializer;
import se.sics.ktoolbox.util.result.ResultSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicSerializerSetup {

     //You may add up to max serializers without the need to recompile all the projects that use the serializer space after gvod
    public static int maxSerializers = 20;
    public static final int serializerIds = 19;

    public static enum BasicSerializers {
        SimpleByteIdentifier(SimpleByteId.class, "simpleByteIdentifierSerializer"),
        StringByteIdentifier(StringByteId.class, "stringByteIdentifierSerializer"),
        IntIdentifier(IntId.class, "intIdentifierSerializer"),
        UUIDIdentifier(UUIDId.class, "uuidIdentifierSerializer"),
        OverlayIdentifier(OverlayId.class, "overlayIdentifierSerializer"),
        BasicAddress(BasicAddress.class, "basicAddressSerializer"),
        NatAwareAddressImpl(NatAwareAddressImpl.class, "strippedNAAddressSerializer"),
        BasicHeader(BasicHeader.class, "basicHeaderSerializer"),
        DecoratedHeader(DecoratedHeader.class, "decoratedHeaderSerializer"),
        BasicContentMsg(BasicContentMsg.class, "basicContentMsgSerializer"),
        NatType(NatType.class, "natTypeSerializer"),
        ResultStatusSerializer(ResultSerializer.class, "resultSerializer"),
        ConnIdsInstanceId(ConnIds.InstanceId.class, "connIdsInstanceIdSerializer"),
        ConnIdsConnId(ConnIds.ConnId.class, "connIdsConnIdSerializer"),
        ConnBaseClientStatus(ConnStatus.BaseClient.class, "connBaseClientStatus"),
        ConnBaseServerStatus(ConnStatus.BaseServer.class, "connBaseServerStatus"),
        ConnMsgsClient(ConnMsgs.Client.class, "connMsgsClientSerializer"),
        ConnMsgsServer(ConnMsgs.Server.class, "connMsgsServerSerializer"),
        ConnEmptyState(ConnState.Empty.class, "connEmptyState");
                
        public final Class serializedClass;
        public final String serializerName;

        BasicSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static boolean checkSetup() {
        for (BasicSerializers bs : BasicSerializers.values()) {
            if (Serializers.lookupSerializer(bs.serializedClass) == null) {
                return false;
            }
        }
        return true;
    }

    public static int registerBasicSerializers(int startingId) {
        if (startingId < 128) {
            throw new RuntimeException("start your serializer ids at 128");
        }
        int currentId = startingId;
        
        SimpleByteIdSerializer simpleByteIdentifierSerializer = new SimpleByteIdSerializer(currentId++);
        Serializers.register(simpleByteIdentifierSerializer, BasicSerializers.SimpleByteIdentifier.serializerName);
        Serializers.register(BasicSerializers.SimpleByteIdentifier.serializedClass, BasicSerializers.SimpleByteIdentifier.serializerName);
        
        StringByteIdSerializer stringByteIdentifierSerializer = new StringByteIdSerializer(currentId++);
        Serializers.register(stringByteIdentifierSerializer, BasicSerializers.StringByteIdentifier.serializerName);
        Serializers.register(BasicSerializers.StringByteIdentifier.serializedClass, BasicSerializers.StringByteIdentifier.serializerName);

        IntIdSerializer intIdentifierSerializer = new IntIdSerializer(currentId++);
        Serializers.register(intIdentifierSerializer, BasicSerializers.IntIdentifier.serializerName);
        Serializers.register(BasicSerializers.IntIdentifier.serializedClass, BasicSerializers.IntIdentifier.serializerName);

        UUIDIdSerializer uuidIdentifierSerializer = new UUIDIdSerializer(currentId++);
        Serializers.register(uuidIdentifierSerializer, BasicSerializers.UUIDIdentifier.serializerName);
        Serializers.register(BasicSerializers.UUIDIdentifier.serializedClass, BasicSerializers.UUIDIdentifier.serializerName);

        Class overlayIdType = IdentifierRegistryV2.idType(BasicIdentifiers.Values.OVERLAY);
        OverlayIdSerializer overlayIdentifierSerializer = new OverlayIdSerializer(currentId++, overlayIdType);
        Serializers.register(overlayIdentifierSerializer, BasicSerializers.OverlayIdentifier.serializerName);
        Serializers.register(BasicSerializers.OverlayIdentifier.serializedClass, BasicSerializers.OverlayIdentifier.serializerName);

        Class nodeIdType = IdentifierRegistryV2.idType(BasicIdentifiers.Values.NODE);
        BasicAddressSerializer basicAddressSerializer = new BasicAddressSerializer(currentId++, nodeIdType);
        Serializers.register(basicAddressSerializer, BasicSerializers.BasicAddress.serializerName);
        Serializers.register(BasicSerializers.BasicAddress.serializedClass, BasicSerializers.BasicAddress.serializerName);

        NatAwareAddressImplSerializer natAwareAddressSerializer = new NatAwareAddressImplSerializer(currentId++);
        Serializers.register(natAwareAddressSerializer, BasicSerializers.NatAwareAddressImpl.serializerName);
        Serializers.register(BasicSerializers.NatAwareAddressImpl.serializedClass, BasicSerializers.NatAwareAddressImpl.serializerName);

        BasicHeaderSerializer basicHeaderSerializer = new BasicHeaderSerializer(currentId++);
        Serializers.register(basicHeaderSerializer, BasicSerializers.BasicHeader.serializerName);
        Serializers.register(BasicSerializers.BasicHeader.serializedClass, BasicSerializers.BasicHeader.serializerName);

        DecoratedHeaderSerializer decoratedHeaderSerializer = new DecoratedHeaderSerializer(currentId++);
        Serializers.register(decoratedHeaderSerializer, BasicSerializers.DecoratedHeader.serializerName);
        Serializers.register(BasicSerializers.DecoratedHeader.serializedClass, BasicSerializers.DecoratedHeader.serializerName);

        BasicContentMsgSerializer basicContentMsgSerializer = new BasicContentMsgSerializer(currentId++);
        Serializers.register(basicContentMsgSerializer, BasicSerializers.BasicContentMsg.serializerName);
        Serializers.register(BasicSerializers.BasicContentMsg.serializedClass, BasicSerializers.BasicContentMsg.serializerName);

        NatTypeSerializer natTypeSerializer = new NatTypeSerializer(currentId++);
        Serializers.register(natTypeSerializer, BasicSerializers.NatType.serializerName);
        Serializers.register(BasicSerializers.NatType.serializedClass, BasicSerializers.NatType.serializerName);
        
        ResultSerializer.Status resultSerializer = new ResultSerializer.Status(currentId++);
        Serializers.register(resultSerializer, BasicSerializers.ResultStatusSerializer.serializerName);
        Serializers.register(BasicSerializers.ResultStatusSerializer.serializedClass, BasicSerializers.ResultStatusSerializer.serializerName);
        
        Serializers.register(new ConnIdsSerializer.InstanceId(currentId++), BasicSerializers.ConnIdsInstanceId.serializerName);
        Serializers.register(BasicSerializers.ConnIdsInstanceId.serializedClass, 
          BasicSerializers.ConnIdsInstanceId.serializerName);
        
        Serializers.register(new ConnIdsSerializer.ConnId(currentId++), BasicSerializers.ConnIdsConnId.serializerName);
        Serializers.register(BasicSerializers.ConnIdsConnId.serializedClass, 
          BasicSerializers.ConnIdsConnId.serializerName);
        
        Serializers.register(new ConnStatusSerializer.BaseClient(currentId++), 
          BasicSerializers.ConnBaseClientStatus.serializerName);
        Serializers.register(BasicSerializers.ConnBaseClientStatus.serializedClass, 
          BasicSerializers.ConnBaseClientStatus.serializerName);
        
        Serializers.register(new ConnStatusSerializer.BaseServer(currentId++), 
          BasicSerializers.ConnBaseServerStatus.serializerName);
        Serializers.register(BasicSerializers.ConnBaseServerStatus.serializedClass, 
          BasicSerializers.ConnBaseServerStatus.serializerName);
        
        Serializers.register(new ConnMsgsSerializer.Client(currentId++), BasicSerializers.ConnMsgsClient.serializerName);
        Serializers.register(BasicSerializers.ConnMsgsClient.serializedClass, 
          BasicSerializers.ConnMsgsClient.serializerName);
        
        Serializers.register(new ConnMsgsSerializer.Server(currentId++), BasicSerializers.ConnMsgsServer.serializerName);
        Serializers.register(BasicSerializers.ConnMsgsServer.serializedClass, 
          BasicSerializers.ConnMsgsServer.serializerName);
        
        Serializers.register(new EmptyConnStateSerializer(currentId++), BasicSerializers.ConnEmptyState.serializerName);
        Serializers.register(BasicSerializers.ConnEmptyState.serializedClass, 
          BasicSerializers.ConnEmptyState.serializerName);
        
        assert startingId + serializerIds == currentId;
        assert serializerIds <= maxSerializers;
        return startingId + maxSerializers;
    }
}
