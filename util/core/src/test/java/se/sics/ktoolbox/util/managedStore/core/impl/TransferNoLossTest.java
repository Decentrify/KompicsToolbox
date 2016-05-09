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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.impl.util.PrepDwnlInfo;
import se.sics.ktoolbox.util.managedStore.core.util.FileInfo;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;
import se.sics.ktoolbox.util.managedStore.core.util.Torrent;
import se.sics.ktoolbox.util.managedStore.core.util.TorrentInfo;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TransferNoLossTest {

    private static final Logger LOG = LoggerFactory.getLogger(TransferNoLossTest.class);

    String uploadHashPath = "./src/test/resources/storageTest1/uploadFile.hash";
    String uploadFilePath = "./src/test/resources/storageTest1/uploadFile.mp4";
    String download1HashPath = "./src/test/resources/storageTest1/downloadFile1.hash";
    String download1FilePath = "./src/test/resources/storageTest1/downloadFile1.txt";
    String torrentName = "torrent1";
    String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
    int fileBlockSize = 100;
    int pieceSize = 1024;
    int piecesPerBlock = 1024;
    int blockSize = pieceSize * piecesPerBlock;

    public void setup(Random rand) throws IOException, HashUtil.HashBuilderException {
        File uploadFile = new File(uploadFilePath);
        uploadFile.getParentFile().mkdirs();
        uploadFile.createNewFile();
        generateFile(uploadFile, rand);
        HashUtil.makeHashes(uploadFilePath, uploadHashPath, hashAlg, blockSize);
    }

    private void generateFile(File file, Random rand) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        for (int i = 0; i < fileBlockSize - 1; i++) {
            byte[] data = new byte[blockSize];
            rand.nextBytes(data);
            out.write(data);
        }
        byte[] data = new byte[blockSize - 1];
        rand.nextBytes(data);
        out.write(data);
        LOG.info("generatedSize:{}", fileBlockSize);
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
    }

    @Test
    public void inOrderTest() throws IOException, HashUtil.HashBuilderException {
        LOG.info("in order test");
        cleanup();
        Random rand = new Random(1234);
        setup(rand);
        runSimple();
        cleanup();
    }

    private void runSimple() throws IOException {
        File uploadFile = new File(uploadFilePath);
        long fileSize = uploadFile.length();
        File hashFile = new File(uploadHashPath);
        long hashFileSize = hashFile.length();
        PrepDwnlInfo prepInfo = new PrepDwnlInfo(5, 1, 5, 5);
        int playPos, blocks;
        double percentage;

        HashMngr uploadHashMngr = StorageMngrFactory.completeMMHashMngr(uploadHashPath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        FileMngr uploadFileMngr = StorageMngrFactory.completeMMFileMngr(uploadFilePath, fileSize, blockSize, pieceSize);

        Torrent torrent1 = new Torrent(new IntIdentifier(1), FileInfo.newFile(torrentName, fileSize), new TorrentInfo(pieceSize, piecesPerBlock, hashAlg, hashFileSize));
        HashMngr download1HashMngr = StorageMngrFactory.incompleteMMHashMngr(download1HashPath, hashAlg, hashFileSize, HashUtil.getHashSize(hashAlg));
        FileMngr download1FileMngr = StorageMngrFactory.incompleteMMFileMngr(download1FilePath, fileSize, blockSize, pieceSize);
        TransferMngr download1Mngr = new TransferMngr(torrent1, download1HashMngr, download1FileMngr);

        LOG.info("running...");
        LOG.info("start transfer");
        playPos = 0;
        Assert.assertEquals(piecesPerBlock + prepInfo.maxHashMsg, download1Mngr.prepareDownload(playPos, prepInfo));
        download("1", piecesPerBlock - 1, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr);
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(5, download1HashMngr.contiguous(0));
        Assert.assertEquals(0, download1FileMngr.contiguous(playPos));
        percentage = 0;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        LOG.info("complete block");
        download("1", 1, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr);
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(5, download1HashMngr.contiguous(0));
        Assert.assertEquals(1, download1FileMngr.contiguous(playPos));
        percentage = (double)1/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        LOG.info("multiple blocks");
        blocks = 4;
        Assert.assertEquals(blocks * piecesPerBlock + 1, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(10, download1HashMngr.contiguous(0));
        Assert.assertEquals(5, download1FileMngr.contiguous(playPos));
        percentage = (double)5/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        LOG.info("advance hash");
        blocks = 2;
        Assert.assertEquals(blocks * piecesPerBlock + 2, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(20, download1HashMngr.contiguous(0));
        Assert.assertEquals(7, download1FileMngr.contiguous(playPos));
        percentage = (double)7/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 50;
        LOG.info("jump 1 - 0, 1");
        blocks = 2;
        Assert.assertEquals(blocks * piecesPerBlock + (50 - 20) / 5 + 2, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(60, download1HashMngr.contiguous(0));
        Assert.assertEquals(52, download1FileMngr.contiguous(playPos));
        percentage = (double)9/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        LOG.info("advance hash");
        blocks = 6;
        Assert.assertEquals(blocks * piecesPerBlock + 3, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(75, download1HashMngr.contiguous(0));
        Assert.assertEquals(58, download1FileMngr.contiguous(playPos));
        percentage = (double)15/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 25;
        LOG.info("jump 2 - 0, 2, 1");
        blocks = 6;
        Assert.assertEquals(blocks * piecesPerBlock + 1, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(80, download1HashMngr.contiguous(0));
        Assert.assertEquals(31, download1FileMngr.contiguous(playPos));
        percentage = (double)21/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 23;
        LOG.info("jump 3 - 0, 3, 2, 1");
        blocks = 6;
        Assert.assertEquals(blocks * piecesPerBlock + 4, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(-1, download1HashMngr.contiguous(0));
        Assert.assertEquals(35, download1FileMngr.contiguous(playPos));
        percentage = (double)27/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 98;
        LOG.info("jump 4(end) - 0, 3, 2, 1, 4");
        blocks = 6;
        Assert.assertEquals(blocks * piecesPerBlock, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(-1, download1HashMngr.contiguous(0));
        Assert.assertEquals(-1, download1FileMngr.contiguous(playPos));
        Assert.assertEquals(11, download1FileMngr.contiguous(0));
        percentage = (double)33/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 0;
        LOG.info("merge download ranges");
        blocks = 12;
        Assert.assertEquals(blocks * piecesPerBlock, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(-1, download1HashMngr.contiguous(0));
        Assert.assertEquals(35, download1FileMngr.contiguous(playPos));
        percentage = (double)45/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 0;
        LOG.info("merge download ranges");
        blocks = 15;
        Assert.assertEquals(blocks * piecesPerBlock, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(-1, download1HashMngr.contiguous(0));
        Assert.assertEquals(58, download1FileMngr.contiguous(playPos));
        percentage = (double)60/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 0;
        LOG.info("merge download ranges");
        blocks = 39;
        Assert.assertEquals(blocks * piecesPerBlock, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(-1, download1HashMngr.contiguous(0));
        Assert.assertEquals(97, download1FileMngr.contiguous(playPos));
        percentage = (double)99/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);

        playPos = 0;
        LOG.info("finish download");
        blocks = 1;
        Assert.assertEquals(blocks * piecesPerBlock, multiBlockDownload("1", blocks, playPos, prepInfo, uploadHashMngr, uploadFileMngr, download1Mngr));
        download1Mngr.checkCompleteBlocks();
        Assert.assertEquals(-1, download1HashMngr.contiguous(0));
        Assert.assertEquals(-1, download1FileMngr.contiguous(playPos));
        percentage = (double)100/fileBlockSize;
        Assert.assertEquals(percentage, download1FileMngr.percentageCompleted(), (double) 0);
    }

    private int multiBlockDownload(String logPrefix, int blocks, int playPos, PrepDwnlInfo prepInfo,
            HashMngr sourceHashMngr, FileMngr sourceFileMngr, TransferMngr destinationMngr) {
        int nrMsgs = 0;
        for (int i = 0; i < blocks; i++) {
            nrMsgs += destinationMngr.prepareDownload(playPos, prepInfo);
            download("1", piecesPerBlock, prepInfo, sourceHashMngr, sourceFileMngr, destinationMngr);
        }
        return nrMsgs;
    }

    private void download(String logPrefix, int pieces, PrepDwnlInfo prepInfo,
            HashMngr sourceHashMngr, FileMngr sourceFileMngr, TransferMngr destinationMngr) {
        Pair<Set<Integer>, Set<Integer>> preparedReq = TransferHelper.prepareTransferReq(logPrefix, destinationMngr, pieces, prepInfo);
        Pair<Map<Integer, ByteBuffer>, Set<Integer>> hashesResp = TransferHelper.prepareHashesResp(logPrefix, sourceHashMngr, preparedReq.getValue0());
        Pair<Map<Integer, ByteBuffer>, Set<Integer>> piecesResp = TransferHelper.preparePiecesResp(logPrefix, sourceFileMngr, preparedReq.getValue1());
        TransferHelper.write(logPrefix, destinationMngr, hashesResp, piecesResp);
    }
}
