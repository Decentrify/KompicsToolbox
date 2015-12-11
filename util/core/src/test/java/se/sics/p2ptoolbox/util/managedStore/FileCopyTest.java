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
package se.sics.p2ptoolbox.util.managedStore;

import se.sics.ktoolbox.util.managedStore.FileMngr;
import se.sics.ktoolbox.util.managedStore.HashUtil;
import se.sics.ktoolbox.util.managedStore.BlockMngr;
import se.sics.ktoolbox.util.managedStore.HashMngr;
import se.sics.ktoolbox.util.managedStore.StorageMngrFactory;
import com.google.common.io.BaseEncoding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FileCopyTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileCopyTest.class);

    String hashFilePath = "./src/test/resources/storageTest1/uploadFile.hash";
    String uploadFilePath = "./src/test/resources/storageTest1/uploadFile.mp4";
    String download1FilePath = "./src/test/resources/storageTest1/downloadFile1.txt";
    String download2FilePath = "./src/test/resources/storageTest1/downloadFile2.txt";
    String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
    int generateSize = 5;
    int pieceSize = 1024;
    int blockSize = 1024 * 1024;
    int randomRuns = 1;
    int nrBlocks;

    public void setup(Random rand) throws IOException, HashUtil.HashBuilderException {
        cleanup(rand);
        File uploadFile = new File(uploadFilePath);
        uploadFile.getParentFile().mkdirs();
        uploadFile.createNewFile();
        generateFile(uploadFile, rand);
        nrBlocks = StorageMngrFactory.nrPieces(uploadFile.length(), blockSize);
        HashUtil.makeHashes(uploadFilePath, hashFilePath, hashAlg, blockSize);
    }

    private void generateFile(File file, Random rand) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        for (int i = 0; i < generateSize; i++) {
            byte[] data = new byte[blockSize];
            rand.nextBytes(data);
            out.write(data);
//            LOG.info("{} block:{}", i, BaseEncoding.base16().encode(data));
        }
        byte[] data = new byte[blockSize - 1];
        rand.nextBytes(data);
        out.write(data);
//        LOG.info("{} block:{}", generateSize, BaseEncoding.base16().encode(data));
        out.flush();
        out.close();
    }

    public void cleanup(Random rand) {
        File uploadFile = new File(uploadFilePath);
        uploadFile.delete();
        File hashFile = new File(hashFilePath);
        hashFile.delete();
        File download1File = new File(download1FilePath);
        download1File.delete();
        File download2File = new File(download2FilePath);
        download2File.delete();
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
            run(rand, new RandomDownloader(hashAlg, rand, pieceSize, new RandomBlockSetter()), 100, 0.3);
            cleanup(rand);
        }
    }

    @Test
    public void testReverse() throws IOException, HashUtil.HashBuilderException {
        int seed = 1234;
        LOG.info("reverse test seed:{}", seed);
        Random rand = new Random(seed);
        setup(rand);
        run(rand, new RandomDownloader(hashAlg, rand, pieceSize, new ReverseBlockSetter()), 100, 0);
        cleanup(rand);
    }

    public void run(Random rand, Downloader downloader, int maxInterleavingSize, double lossRate) throws IOException {
        File uploadFile = new File(uploadFilePath);
        long fileSize = uploadFile.length();
        File hashFile = new File(hashFilePath);
        long hashFileSize = hashFile.length();
        int nrBlocks = StorageMngrFactory.nrPieces(fileSize, blockSize);

        HashMngr hashMngr = StorageMngrFactory.getCompleteHashMngr(hashFilePath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        FileMngr uploadFileMngr = StorageMngrFactory.getCompleteFileMngr(uploadFilePath, fileSize, blockSize, pieceSize);
        FileMngr download1FileMngr = StorageMngrFactory.getIncompleteFileMngr(download1FilePath, fileSize, blockSize, pieceSize);
        FileMngr download2FileMngr = StorageMngrFactory.getIncompleteFileMngr(download2FilePath, fileSize, blockSize, pieceSize);

        BlockMngr downloadBlock;
        downloadBlock = StorageMngrFactory.getSimpleBlockMngr(download1FileMngr.blockSize(0), pieceSize);
        Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> download1 = Pair.with(download1FileMngr, Triplet.with(0, downloadBlock, new ArrayList<Integer>()));
        downloader.populateBlock(download1.getValue1());
        downloadBlock = StorageMngrFactory.getSimpleBlockMngr(download2FileMngr.blockSize(0), pieceSize);
        Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> download2 = Pair.with(download2FileMngr, Triplet.with(0, downloadBlock, new ArrayList<Integer>()));
        downloader.populateBlock(download2.getValue1());

//        LOG.info("status file1:{} file2:{} pPieces1:{} pPieces2:{}",
//                new Object[]{download1FileMngr.contiguous(0), download2FileMngr.contiguous(0), download1.getValue2().size(), download2.getValue2().size()});
        while (!(download1FileMngr.isComplete(0) && download2FileMngr.isComplete(0))) {
            if (!download1FileMngr.isComplete(0)) {
//                LOG.info("file1 block:{}", download1.getValue1().getValue0());
                downloader.download(lossRate, rand.nextInt(maxInterleavingSize), uploadFileMngr, download1);
                download1 = downloader.processIntermediate(rand.nextInt(nrBlocks), download1, hashMngr);
            }
            if (!download2FileMngr.isComplete(0)) {
//                LOG.info("file2 block:{}", download2.getValue1().getValue0());
                downloader.download(lossRate, rand.nextInt(maxInterleavingSize), download1FileMngr, download2);
                download2 = downloader.processIntermediate(rand.nextInt(nrBlocks), download2, hashMngr);
            }
//            LOG.info("status file1:{} file2:{}", download1FileMngr.contiguous(0), download2FileMngr.contiguous(0));
        }
//        LOG.info("done1:{} done2:{}", download1FileMngr.isComplete(0), download2FileMngr.isComplete(0));
    }

    public static interface Downloader {

        public void populateBlock(Triplet<Integer, BlockMngr, ArrayList<Integer>> blockInfo);

        public void download(double lossRate, int windowSize, FileMngr uploader, Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> downloader);

        public Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> processIntermediate(int blockPos, Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> state, HashMngr hashMngr);
    }

    public class RandomDownloader implements Downloader {

        private String hashAlg;
        private Random rand;
        private int pieceSize;
        private BlockSetter bs;

        public RandomDownloader(String hashAlg, Random rand, int pieceSize, BlockSetter bs) {
            this.hashAlg = hashAlg;
            this.rand = rand;
            this.pieceSize = pieceSize;
            this.bs = bs;
        }

        public void populateBlock(Triplet<Integer, BlockMngr, ArrayList<Integer>> blockInfo) {
//            LOG.info("nr of pieces:{}", peer.getValue1().nrPieces());
            for (int i = 0; i < blockInfo.getValue1().nrPieces(); i++) {
                blockInfo.getValue2().add(i);
            }
            bs.setPieces(rand, blockInfo.getValue2());
        }

        public void download(double lossRate, int windowSize, FileMngr uploader, Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> downloader) {
            int blockNr = downloader.getValue1().getValue0();
            ArrayList<Integer> nextPieces = downloader.getValue1().getValue2();
            BlockMngr block = downloader.getValue1().getValue1();
            while (windowSize > 0) {
                if (nextPieces.isEmpty()) {
                    return;
                }
                int nextPiece = nextPieces.remove(0);
                int pieceNr = blockNr * pieceSize + nextPiece;
                windowSize--;
                if (uploader.hasPiece(pieceNr)) {
                    if (rand.nextDouble() > lossRate) {
                        byte[] piece = uploader.readPiece(pieceNr);
                        block.writePiece(nextPiece, piece);
                        continue;
                    }
                }
                nextPieces.add(nextPiece);
            }
        }

        public Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> processIntermediate(int blockPos, Pair<FileMngr, Triplet<Integer, BlockMngr, ArrayList<Integer>>> state, HashMngr hashMngr) {
            FileMngr fileMngr = state.getValue0();
            Triplet<Integer, BlockMngr, ArrayList<Integer>> blockInfo = state.getValue1();

            if (blockInfo.getValue1().isComplete()) {
//                LOG.info("block:{} completed of:{}", blockInfo.getValue0(), nrBlocks);
                if (!blockInfo.getValue2().isEmpty()) {
                    throw new RuntimeException("pending pieces error");
                }
                byte[] block = blockInfo.getValue1().getBlock();
//                LOG.info("block:{} val:{}", blockInfo.getValue0(), BaseEncoding.base16().encode(block));
                if (!HashUtil.checkHash(hashAlg, block, hashMngr.readHash(blockInfo.getValue0()))) {
                    LOG.error("hash:{} mismatch", blockInfo.getValue0());
                    throw new RuntimeException("hash missmatch");
                } else {
                    fileMngr.writeBlock(blockInfo.getValue0(), block);
                    if (fileMngr.isComplete(0)) {
                        return Pair.with(fileMngr, null);
                    }
//                    LOG.info("block nr:{} size:{}", blockNr, fileMngr.blockSize(blockNr));
                    int blockNr = fileMngr.nextBlock(blockPos, new HashSet<Integer>());
                    if (blockNr >= nrBlocks) {
                        blockNr = fileMngr.nextBlock(0, new HashSet<Integer>());
                        if (blockNr >= nrBlocks) {
                            return state;
                        }
                    }
//                    LOG.info("completed:{} next:{}", state.getValue1().getValue0(), blockNr);
                    blockInfo = Triplet.with(blockNr, StorageMngrFactory.getSimpleBlockMngr(fileMngr.blockSize(blockNr), pieceSize), new ArrayList<Integer>());
                    populateBlock(blockInfo);
                    return Pair.with(fileMngr, blockInfo);
                }
            }
            return state;
        }
    }

    public static interface BlockSetter {

        public ArrayList<Integer> setPieces(Random rand, ArrayList<Integer> pieces);
    }

    public static class RandomBlockSetter implements BlockSetter {

        public ArrayList<Integer> setPieces(Random rand, ArrayList<Integer> pieces) {
            Collections.shuffle(pieces, rand);
            return pieces;
        }
    }

    public static class ReverseBlockSetter implements BlockSetter {

        public ArrayList<Integer> setPieces(Random rand, ArrayList<Integer> pieces) {
            Collections.reverse(pieces);
            return pieces;
        }
    }
}
