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
package se.sics.ktoolbox.util.managedStore.core.impl;

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DownloadMngrTest {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadMngrTest.class);

    String uploadHashPath = "./src/test/resources/storageTest1/uploadFile.hash";
    String uploadFilePath = "./src/test/resources/storageTest1/uploadFile.mp4";
    String download1HashPath = "./src/test/resources/storageTest1/downloadFile1.hash";
    String download1FilePath = "./src/test/resources/storageTest1/downloadFile1.txt";
    String download2HashPath = "./src/test/resources/storageTest1/downloadFile2.hash";
    String download2FilePath = "./src/test/resources/storageTest1/downloadFile2.txt";
    String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
    int generateSize = 5;
    int pieceSize = 1024;
    int piecesPerBlock = 1024;
    int blockSize = pieceSize * piecesPerBlock;
    int hashesPerMsg = 20;
    int randomRuns = 1;

    public void setup(Random rand) throws IOException, HashUtil.HashBuilderException {
        File uploadFile = new File(uploadFilePath);
        uploadFile.getParentFile().mkdirs();
        uploadFile.createNewFile();
        generateFile(uploadFile, rand);
        HashUtil.makeHashes(uploadFilePath, uploadHashPath, hashAlg, blockSize);
    }

    private void generateFile(File file, Random rand) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        for (int i = 0; i < generateSize - 1; i++) {
            byte[] data = new byte[blockSize];
            rand.nextBytes(data);
            out.write(data);
        }
        byte[] data = new byte[blockSize - 1];
        rand.nextBytes(data);
        out.write(data);
        LOG.info("generatedSize:{}", generateSize);
        out.flush();
        out.close();
    }

    public void cleanup() {
        File uploadFile = new File(uploadFilePath);
        uploadFile.delete();
        File uploadHash = new File(uploadHashPath);
        uploadHash.delete();
        File download1File = new File(download1FilePath);
        download1File.delete();
        File download1Hash = new File(download1HashPath);
        download1Hash.delete();
        File download2File = new File(download2FilePath);
        download2File.delete();
        File download2Hash = new File(download2HashPath);
        download2Hash.delete();
    }

//    @Test
    public void testRandom() throws IOException, HashUtil.HashBuilderException {
        Random rand;
        Random seedGen = new SecureRandom();
        for (int i = 0; i < randomRuns; i++) {
            long seed = seedGen.nextInt();
            LOG.info("random test seed:{}", seed);
            cleanup();
            rand = new Random(seed);
            setup(rand);
            run(rand, 0.3, 0.1);
            cleanup();
        }
    }

    @Test
    public void simpleInOrderNoLossNoMalformTest() throws IOException, HashUtil.HashBuilderException {
        long seed = 1024;
        LOG.info("random test seed:{}", seed);
        cleanup();
        Random rand = new Random(seed);
        setup(rand);
        run(rand, 0, 0);
        cleanup();
    }

    private void run(Random rand, double lossRate, double malformRate) throws IOException {
        File uploadFile = new File(uploadFilePath);
        long fileSize = uploadFile.length();
        File hashFile = new File(uploadHashPath);
        long hashFileSize = hashFile.length();

        HashMngr uploadHashMngr = StorageMngrFactory.completeMMHashMngr(uploadHashPath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        FileMngr uploadFileMngr = StorageMngrFactory.completeMMFileMngr(uploadFilePath, fileSize, blockSize, pieceSize);

        HashMngr download1HashMngr = StorageMngrFactory.incompleteMMHashMngr(download1HashPath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        FileMngr download1FileMngr = StorageMngrFactory.incompleteMMFileMngr(download1FilePath, fileSize, blockSize, pieceSize);
        DownloadMngr download1Mngr = new DownloadMngr(download1HashMngr, download1FileMngr, hashAlg, pieceSize, piecesPerBlock, hashesPerMsg);

        HashMngr download2HashMngr = StorageMngrFactory.incompleteMMHashMngr(download2HashPath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        FileMngr download2FileMngr = StorageMngrFactory.incompleteMMFileMngr(download2FilePath, fileSize, blockSize, pieceSize);
        DownloadMngr download2Mngr = new DownloadMngr(download2HashMngr, download2FileMngr, hashAlg, pieceSize, piecesPerBlock, hashesPerMsg);

        while (!(download1FileMngr.isComplete(0) && download2FileMngr.isComplete(0))) {
            download(1, uploadHashMngr, uploadFileMngr, download1Mngr, 50);
            download(2, download1HashMngr, download1FileMngr, download2Mngr, 70);
            LOG.info("1: hash:{} file:{}", download1HashMngr.contiguous(0), download1FileMngr.contiguous(0));
            LOG.info("2: hash:{} file:{}", download2HashMngr.contiguous(0), download2FileMngr.contiguous(0));
            download1Mngr.checkCompleteBlocks();
            download2Mngr.checkCompleteBlocks();
        }
    }

    private void download(int dwnlId, HashMngr sourceHashMngr, FileMngr sourceFileMngr, DownloadMngr destinationMngr, int pieces) {
        destinationMngr.download(0, 0, 5, 10);

        Optional<Set<Integer>> nextHashes = destinationMngr.downloadHash();
        if (nextHashes.isPresent()) {
            Map<Integer, ByteBuffer> hashes = new HashMap<>();
            Set<Integer> missingHashes = new TreeSet<>();
            for (Integer hashNr : nextHashes.get()) {
                if (sourceHashMngr.hasHash(hashNr)) {
                    hashes.put(hashNr, ByteBuffer.wrap(sourceHashMngr.readHash(hashNr)));
                } else {
                    missingHashes.add(hashNr);
                }
            }
//            LOG.debug("{}:hashes:{} missing:{}", new Object[]{dwnlId, hashes.keySet(), missingHashes});
            destinationMngr.putHashes(hashes, missingHashes);
        }
        for (int i = 0; i < pieces; i++) {
            Optional<Integer> nextPiece = destinationMngr.downloadData();
            if (nextPiece.isPresent()) {
                if (nextPiece.get() == -1) {
                    throw new RuntimeException("should not get -1 here");
                }
//                LOG.debug("{}:getting piece:{}", dwnlId, nextPiece.get());
                if (sourceFileMngr.hasPiece(nextPiece.get())) {
//                    LOG.debug("{}:got piece:{}", new Object[]{dwnlId, nextPiece.get()});
                    ByteBuffer value = ByteBuffer.wrap(sourceFileMngr.readPiece(nextPiece.get()));
                    destinationMngr.putPiece(nextPiece.get(), value);
                } else {
                    destinationMngr.resetPiece(nextPiece.get());
                }
            }
        }
    }
}
