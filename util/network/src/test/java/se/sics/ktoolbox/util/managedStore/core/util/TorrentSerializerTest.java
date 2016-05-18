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
package se.sics.ktoolbox.util.managedStore.core.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TorrentSerializerTest {
    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
    }
    
    @Test
    public void testSimple() {
        Serializer serializer = Serializers.lookupSerializer(Torrent.class);
        Torrent original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        Identifier overlayId = new IntIdentifier(10);
        FileInfo fileInfo = FileInfo.newFile("file1", 1024*1024);
        String shaHash = HashUtil.getAlgName(HashUtil.SHA);
        int hashFileSize = HashUtil.getHashSize(shaHash) * 10;
        TorrentInfo torrentInfo = new TorrentInfo(1024, 1000, shaHash, hashFileSize);
        original = new Torrent(overlayId, fileInfo, torrentInfo);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);

        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (Torrent) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original.overlayId, copy.overlayId);
        Assert.assertEquals(original.fileInfo.name, copy.fileInfo.name);
        Assert.assertEquals(original.fileInfo.size, copy.fileInfo.size);
        Assert.assertEquals(original.torrentInfo.hashAlg, copy.torrentInfo.hashAlg);
        Assert.assertEquals(original.torrentInfo.hashFileSize, copy.torrentInfo.hashFileSize);
        Assert.assertEquals(original.torrentInfo.pieceSize, copy.torrentInfo.pieceSize);
        Assert.assertEquals(original.torrentInfo.piecesPerBlock, copy.torrentInfo.piecesPerBlock);
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }
}
