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

import se.sics.ktoolbox.util.network.basic.BasicHeaderSerializer;
import se.sics.ktoolbox.util.network.basic.BasicAddressSerializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifierSerializer;
import se.sics.ktoolbox.util.identifiable.basic.OverlayIdentifier;
import se.sics.ktoolbox.util.identifiable.basic.OverlayIdentifierSerializer;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifierSerializer;
import se.sics.ktoolbox.util.managedStore.core.util.FileInfo;
import se.sics.ktoolbox.util.managedStore.core.util.FileInfoSerializer;
import se.sics.ktoolbox.util.managedStore.core.util.Torrent;
import se.sics.ktoolbox.util.managedStore.core.util.TorrentInfo;
import se.sics.ktoolbox.util.managedStore.core.util.TorrentInfoSerializer;
import se.sics.ktoolbox.util.managedStore.core.util.TorrentSerializer;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImplSerializer;
import se.sics.ktoolbox.util.network.nat.NatType;
import se.sics.ktoolbox.util.network.basic.BasicContentMsgSerializer;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeaderSerializer;
import se.sics.ktoolbox.util.network.nat.NatTypeSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicSerializerSetup {

    public static final int serializerIds = 12;

    public static enum BasicSerializers {

        IntIdentifier(IntIdentifier.class, "intIdentifierSerializer"),
        UUIDIdentifier(UUIDIdentifier.class, "uuidIdentifierSerializer"),
        OverlayIdentifier(OverlayIdentifier.class, "overlayIdentifierSerializer"),
        BasicAddress(BasicAddress.class, "basicAddressSerializer"),
        NatAwareAddressImpl(NatAwareAddressImpl.class, "strippedNAAddressSerializer"),
        BasicHeader(BasicHeader.class, "basicHeaderSerializer"),
        DecoratedHeader(DecoratedHeader.class, "decoratedHeaderSerializer"),
        BasicContentMsg(BasicContentMsg.class, "basicContentMsgSerializer"),
        NatType(NatType.class, "natTypeSerializer"),
        FileInfo(FileInfo.class, "mSfileInfoSerializer"),
        TorrentInfo(TorrentInfo.class, "msTorrentInfoSerializer"),
        Torrent(Torrent.class, "msTorrentSerializer");
                
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

        IntIdentifierSerializer intIdentifierSerializer = new IntIdentifierSerializer(currentId++);
        Serializers.register(intIdentifierSerializer, BasicSerializers.IntIdentifier.serializerName);
        Serializers.register(BasicSerializers.IntIdentifier.serializedClass, BasicSerializers.IntIdentifier.serializerName);

        UUIDIdentifierSerializer uuidIdentifierSerializer = new UUIDIdentifierSerializer(currentId++);
        Serializers.register(uuidIdentifierSerializer, BasicSerializers.UUIDIdentifier.serializerName);
        Serializers.register(BasicSerializers.UUIDIdentifier.serializedClass, BasicSerializers.UUIDIdentifier.serializerName);

        OverlayIdentifierSerializer overlayIdentifierSerializer = new OverlayIdentifierSerializer(currentId++);
        Serializers.register(overlayIdentifierSerializer, BasicSerializers.OverlayIdentifier.serializerName);
        Serializers.register(BasicSerializers.OverlayIdentifier.serializedClass, BasicSerializers.OverlayIdentifier.serializerName);

        BasicAddressSerializer basicAddressSerializer = new BasicAddressSerializer(currentId++);
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
        
        FileInfoSerializer fileInfoSerializer = new FileInfoSerializer(currentId++);
        Serializers.register(fileInfoSerializer, BasicSerializers.FileInfo.serializerName);
        Serializers.register(BasicSerializers.FileInfo.serializedClass, BasicSerializers.FileInfo.serializerName);
        
        TorrentInfoSerializer torrentInfoSerializer = new TorrentInfoSerializer(currentId++);
        Serializers.register(torrentInfoSerializer, BasicSerializers.TorrentInfo.serializerName);
        Serializers.register(BasicSerializers.TorrentInfo.serializedClass, BasicSerializers.TorrentInfo.serializerName);
        
        TorrentSerializer torrentSerializer = new TorrentSerializer(currentId++);
        Serializers.register(torrentSerializer, BasicSerializers.Torrent.serializerName);
        Serializers.register(BasicSerializers.Torrent.serializedClass, BasicSerializers.Torrent.serializerName);

        assert startingId + serializerIds == currentId;
        return currentId;
    }
}
