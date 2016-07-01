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
package se.sics.ktoolbox.hops.managedStore;

import com.google.common.io.BaseEncoding;
import com.typesafe.config.ConfigFactory;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import junit.framework.Assert;
import org.javatuples.Pair;
import org.junit.Test;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.TypesafeConfig;
import se.sics.ktoolbox.hdfs.HDFSResource;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.managedStore.core.BlockMngr;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.impl.StorageMngrFactory;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestCachedHDFSStorage {

    @Test
    public void test1() {
        final int blockLength = 1024 * 1024;
        final int pieceLength = 1024;

        Config config = TypesafeConfig.load(ConfigFactory.load("testCache.conf"));
        String hashAlg = HashUtil.getAlgName(HashUtil.SHA);

        HDFSResource uploadResource = new HDFSResource("bbc1.sics.se", 26801, "glassfish", "/experiment/upload/", "file");
        Pair<FileMngr, HashMngr> upload = HopsFactory.getCompleteCached(config, uploadResource, null, hashAlg, blockLength, pieceLength);
        HDFSResource downloadResource = new HDFSResource("bbc1.sics.se", 26801, "glassfish", "/experiment/download/", "file");
        Pair<FileMngr, HashMngr> download = HopsFactory.getIncompleteCached(config, downloadResource, null, 100 * 1000 * 1000, hashAlg, blockLength, pieceLength);

        ByteBuffer hash, block, piece;
        Identifier readerId = new IntIdentifier(0);
        Set<Integer> hashes = new HashSet<>();
        Set<Integer> bufferBlocks = new HashSet<>();
        int blockNr;
        long readPos;

        blockNr = 0;
        readPos = 0;
        bufferBlocks.add(blockNr);
        hashes.add(blockNr);

        upload.getValue1().readHashes(readerId, hashes, bufferBlocks);
        hash = upload.getValue1().readHash(blockNr);
        block = upload.getValue0().read(readerId, readPos, blockLength, bufferBlocks);
        Assert.assertTrue(HashUtil.checkHash(hashAlg, block.array(), hash.array()));

        for (int i = 1; i < 90; i++) {
            blockNr = i;
            readPos = i * 1024 * 1024;
            bufferBlocks.add(blockNr);
            hashes.add(blockNr);

            BlockMngr blockMngr = StorageMngrFactory.inMemoryBlockMngr(blockLength, pieceLength);
            upload.getValue1().readHashes(readerId, hashes, bufferBlocks);
            hash = upload.getValue1().readHash(blockNr);

            for (int j = 0; j < 1024; j++) {
                int pieceNr = blockNr * 1024 + j;
                piece = upload.getValue0().readPiece(readerId, pieceNr, bufferBlocks);
                blockMngr.writePiece(j, piece.array());
            }
            Assert.assertTrue(blockMngr.isComplete());
            block = upload.getValue0().read(readerId, readPos, blockLength, bufferBlocks);

            byte[] blockPrint, blockMngrPrint;
            blockPrint = Arrays.copyOfRange(block.array(), 1000, 1024);
            blockMngrPrint = Arrays.copyOfRange(blockMngr.getBlock(), 1000, 1024);
            System.err.println(BaseEncoding.base16().encode(blockPrint));
            System.err.println(BaseEncoding.base16().encode(blockMngrPrint));
            
            blockPrint = Arrays.copyOfRange(block.array(), 1024, 1034);
            blockMngrPrint = Arrays.copyOfRange(blockMngr.getBlock(), 1024, 1034);
            System.err.println(BaseEncoding.base16().encode(blockPrint));
            System.err.println(BaseEncoding.base16().encode(blockMngrPrint));
            
            Assert.assertTrue(HashUtil.checkHash(hashAlg, block.array(), hash.array()));
            Assert.assertTrue(HashUtil.checkHash(hashAlg, blockMngr.getBlock(), hash.array()));
        }
    }
}
