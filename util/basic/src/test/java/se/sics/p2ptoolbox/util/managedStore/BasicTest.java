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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import org.javatuples.Triplet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicTest {

    private static final Logger LOG = LoggerFactory.getLogger(BasicTest.class);

    String hashFilePath = "./src/test/resources/storageTest1/uploadFile.hash";
    String uploadFilePath = "./src/test/resources/storageTest1/uploadFile.txt";
    String download1FilePath = "./src/test/resources/storageTest1/downloadFile1.txt";
    String download2FilePath = "./src/test/resources/storageTest1/downloadFile2.txt";
    String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
    int generateSize = 5;
    int pieceSize = 1024;
    int blockSize = 1024 * 1024;
    int randomRuns = 100;

    public void setup(Random rand) throws IOException, HashUtil.HashBuilderException {
        cleanup(rand);
        File uploadFile = new File(uploadFilePath);
        uploadFile.getParentFile().mkdirs();
        uploadFile.createNewFile();
        generateFile(uploadFile, rand);
        HashUtil.makeHashes(uploadFilePath, hashFilePath, hashAlg, blockSize);
    }

    private void generateFile(File file, Random rand) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        for (int i = 0; i < generateSize; i++) {
            byte[] data = new byte[blockSize];
            rand.nextBytes(data);
            out.write(data);
        }
        byte[] data = new byte[1500];
        rand.nextBytes(data);
        out.write(data);
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

        HashMngr hashMngr = StorageMngrFactory.getCompleteHashMngr(hashFilePath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        FileMngr uploadFileMngr = StorageMngrFactory.getCompleteFileMngr(uploadFilePath, fileSize, blockSize, pieceSize);
        FileMngr download1FileMngr = StorageMngrFactory.getIncompleteFileMngr(download1FilePath, fileSize, blockSize, pieceSize);
        FileMngr download2FileMngr = StorageMngrFactory.getIncompleteFileMngr(download2FilePath, fileSize, blockSize, pieceSize);

        BlockMngr downloadBlock;
        downloadBlock = StorageMngrFactory.getSimpleBlockMngr(download1FileMngr.blockSize(0), pieceSize);
        Triplet<FileMngr, BlockMngr, ArrayList<Integer>> download1 = Triplet.with(download1FileMngr, downloadBlock, new ArrayList<Integer>());
        downloader.populateBlock(download1);
        downloadBlock = StorageMngrFactory.getSimpleBlockMngr(download2FileMngr.blockSize(0), pieceSize);
        Triplet<FileMngr, BlockMngr, ArrayList<Integer>> download2 = Triplet.with(download2FileMngr, downloadBlock, new ArrayList<Integer>());
        downloader.populateBlock(download2);

//        LOG.info("status file1:{} file2:{} pPieces1:{} pPieces2:{}",
//                new Object[]{download1FileMngr.contiguous(0), download2FileMngr.contiguous(0), download1.getValue2().size(), download2.getValue2().size()});
        while (!(download1FileMngr.isComplete(0) && download2FileMngr.isComplete(0))) {
//            LOG.info("status file1:{} file2:{}", download1FileMngr.contiguous(0), download2FileMngr.contiguous(0));
            if (!download1FileMngr.isComplete(0)) {
//                LOG.info("pending pieces1:{}", download1.getValue2().size());
                downloader.download(lossRate, rand.nextInt(maxInterleavingSize), uploadFileMngr, download1);
                download1 = downloader.processIntermediate(download1, hashMngr);
            }
            if (!download2FileMngr.isComplete(0)) {
//                LOG.info("pending pieces2:{}", download2.getValue2().size());
                downloader.download(lossRate, rand.nextInt(maxInterleavingSize), download1FileMngr, download2);
                download2 = downloader.processIntermediate(download2, hashMngr);
            }
        }
//        LOG.info("done1:{} done2:{}", download1FileMngr.isComplete(0), download2FileMngr.isComplete(0));
    }

    public static interface Downloader {

        public void populateBlock(Triplet<FileMngr, BlockMngr, ArrayList<Integer>> peer);

        public void download(double lossRate, int windowSize, FileMngr uploader, Triplet<FileMngr, BlockMngr, ArrayList<Integer>> downloader);

        public Triplet<FileMngr, BlockMngr, ArrayList<Integer>> processIntermediate(Triplet<FileMngr, BlockMngr, ArrayList<Integer>> state, HashMngr hashMngr);
    }

    public static class RandomDownloader implements Downloader {

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

        public void populateBlock(Triplet<FileMngr, BlockMngr, ArrayList<Integer>> peer) {
//            LOG.info("nr of pieces:{}", peer.getValue1().nrPieces());
            for (int i = 0; i < peer.getValue1().nrPieces(); i++) {
                peer.getValue2().add(i);
            }
            bs.setPieces(rand, peer.getValue2());
        }

        public void download(double lossRate, int windowSize, FileMngr uploader, Triplet<FileMngr, BlockMngr, ArrayList<Integer>> downloader) {
            int blockNr = downloader.getValue0().nextBlock(0, new HashSet<Integer>());
            while (windowSize > 0) {
                if (downloader.getValue2().isEmpty()) {
                    return;
                }
                int nextPiece = downloader.getValue2().remove(0);
                int pieceNr = blockNr * pieceSize + nextPiece;
                windowSize--;
                if (uploader.hasPiece(pieceNr)) {
                    if (rand.nextDouble() > lossRate) {
                        byte[] piece = uploader.readPiece(pieceNr);
                        downloader.getValue1().writePiece(nextPiece, piece);
                        continue;
                    }
                }
                downloader.getValue2().add(nextPiece);
            }
        }

        public Triplet<FileMngr, BlockMngr, ArrayList<Integer>> processIntermediate(Triplet<FileMngr, BlockMngr, ArrayList<Integer>> state, HashMngr hashMngr) {
            Triplet<FileMngr, BlockMngr, ArrayList<Integer>> stateAux = state;
            FileMngr fileMngr = state.getValue0();
            BlockMngr blockMngr = state.getValue1();

            if (blockMngr.isComplete()) {
                int blockNr = fileMngr.nextBlock(0, new HashSet<Integer>());
//                LOG.info("block:{} completed has hash:{}", blockNr, hashMngr.hasHash(blockNr));
                if (!stateAux.getValue2().isEmpty()) {
                    throw new RuntimeException("pending pieces error");
                }
                byte[] block = blockMngr.getBlock();
//            LOG.info("new block:{}", BaseEncoding.base16().encode(block));
                if (!HashUtil.checkHash(hashAlg, block, hashMngr.readHash(blockNr))) {
                    throw new RuntimeException("hash missmatch");
                } else {
                    fileMngr.writeBlock(blockNr, block);
                    blockNr++;
                    if (fileMngr.isComplete(0)) {
                        return Triplet.with(fileMngr, null, null);
                    }
//                    LOG.info("block nr:{} size:{}", blockNr, fileMngr.blockSize(blockNr));
                    blockMngr = StorageMngrFactory.getSimpleBlockMngr(fileMngr.blockSize(blockNr), pieceSize);
                    stateAux = Triplet.with(fileMngr, blockMngr, new ArrayList<Integer>());
                    populateBlock(stateAux);
                }
            }
            return stateAux;
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
