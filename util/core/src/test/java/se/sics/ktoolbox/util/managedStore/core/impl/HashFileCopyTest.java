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
package se.sics.ktoolbox.util.managedStore.core.impl;

import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HashFileCopyTest {

    private static final Logger LOG = LoggerFactory.getLogger(HashFileCopyTest.class);

    String filePath = "./src/test/resources/hashFileCopyTest/file.txt";
    String hashPath = "./src/test/resources/hashFileCopyTest/file.hash";
    String hashCopy1Path = "./src/test/resources/hashFileCopyTest/file.hash1";
    String hashCopy2Path = "./src/test/resources/hashFileCopyTest/file.hash2";
    String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
    int generateSize = 500;
    int blockSize = 1024;
    int randomRuns = 10;

    public void setup(Random rand) throws IOException, HashUtil.HashBuilderException {
        cleanup(rand);
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        file.createNewFile();
        generateFile(file, rand);
        HashUtil.makeHashes(filePath, hashPath, hashAlg, blockSize);
    }

    private void generateFile(File file, Random rand) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            for (int i = 0; i < generateSize; i++) {
                byte[] data = new byte[blockSize];
                rand.nextBytes(data);
                out.write(data);
            }
            byte[] data = new byte[1500];
            rand.nextBytes(data);
            out.write(data);
            out.flush();
        } finally {
            out.close();
        }
    }

    public void cleanup(Random rand) {
        File file;
        file = new File(filePath);
        file.delete();
        file = new File(hashPath);
        file.delete();
        file = new File(hashCopy1Path);
        file.delete();
        file = new File(hashCopy2Path);
        file.delete();
    }

    @Test
    public void testRandom() throws IOException, HashUtil.HashBuilderException {
        Random rand;
        Random seedGen = new SecureRandom();
        for (int i = 0; i < randomRuns; i++) {
            int seed = seedGen.nextInt();
            LOG.info("random test seed:{}", seed);
            rand = new Random(seed);
            setup(rand);
            run(rand, new RandomDownloader(rand), 10, 0.3);
            Assert.assertTrue(Files.equal(new File(hashPath), new File(hashCopy2Path)));
            cleanup(rand);
        }
    }

    public void run(Random rand, Downloader downloader, int maxInterleavingSize, double lossRate) throws IOException {
        File hashFile = new File(hashPath);
        long hashFileSize = hashFile.length();
        int nrHashes = ManagedStoreHelper.componentNr(hashFileSize, HashUtil.getHashSize(hashAlg));

        HashMngr hashMngr = StorageMngrFactory.completeMMHashMngr(hashPath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        HashMngr hashCopy1Mngr = StorageMngrFactory.incompleteMMHashMngr(hashCopy1Path, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        HashMngr hashCopy2Mngr = StorageMngrFactory.incompleteMMHashMngr(hashCopy2Path, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));

//        LOG.info("status file1:{} file2:{} pPieces1:{} pPieces2:{}",
//                new Object[]{download1FileMngr.contiguous(0), download2FileMngr.contiguous(0), download1.getValue2().size(), download2.getValue2().size()});
        while (!(hashCopy1Mngr.isComplete(0) && hashCopy2Mngr.isComplete(0))) {
            if (!hashCopy1Mngr.isComplete(0)) {
//                LOG.info("pending pieces1:{}", download1.getValue2().size());
                downloader.download(rand.nextInt(nrHashes), lossRate, rand.nextInt(maxInterleavingSize), hashMngr, hashCopy1Mngr);
            }
            if (!hashCopy2Mngr.isComplete(0)) {
//                LOG.info("pending pieces2:{}", download2.getValue2().size());
                downloader.download(rand.nextInt(nrHashes), lossRate, rand.nextInt(maxInterleavingSize), hashCopy1Mngr, hashCopy2Mngr);
            }
//            LOG.info("contiguous file1:{} file2:{}", hashCopy1Mngr.contiguous(0), hashCopy2Mngr.contiguous(0));
        }
    }

    public static interface Downloader {

        public void download(int pos, double lossRate, int windowSize, HashMngr uploader, HashMngr downloader);
    }

    public static class RandomDownloader implements Downloader {

        private Random rand;

        public RandomDownloader(Random rand) {
            this.rand = rand;
        }

        public void download(int pos, double lossRate, int windowSize, HashMngr uploader, HashMngr downloader) {
            while (windowSize > 0) {
                if (downloader.isComplete(0)) {
                    return;
                }
                Set<Integer> nextHashes = downloader.nextHashes(windowSize / 3, pos, new HashSet<Integer>());
                if (nextHashes.isEmpty()) {
                    nextHashes = downloader.nextHashes(windowSize / 3, 0, new HashSet<Integer>());
                    if (nextHashes.isEmpty()) {
                        return;
                    }
                }
//                LOG.info("next hashes:{}", nextHashes);
                windowSize -= nextHashes.size();
                for (int hashNr : nextHashes) {
                    if (uploader.hasHash(hashNr)) {
                        if (rand.nextDouble() > lossRate) {
//                            LOG.info("downloading hash:{}", hashNr);
                            ByteBuffer hash = uploader.readHash(hashNr);
                            downloader.writeHash(hashNr, hash.array());
                        }
                    }
                }
            }
        }
    }
}
