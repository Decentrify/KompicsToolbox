///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.util.managedStore.core.impl;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.util.Random;
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.ktoolbox.util.managedStore.core.FileMngr;
//import se.sics.ktoolbox.util.managedStore.core.Storage;
//import se.sics.ktoolbox.util.managedStore.core.impl.storage.RMemMapFile;
//import se.sics.ktoolbox.util.managedStore.core.impl.storage.RWMemMapFile;
//import se.sics.ktoolbox.util.managedStore.core.impl.storage.StorageFactory;
//import se.sics.ktoolbox.util.stream.tracker.IncompleteTracker;
//import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class LBAOFileMngrTest {
//
//    private static final Logger LOG = LoggerFactory.getLogger(LBAOFileMngrTest.class);
//
//    final int pieceSize = 1024;
//    final int piecesPerBlock = 1024;
//    final int blockSize = pieceSize * piecesPerBlock;
//
//    final String testDirPath = "./src/test/resources/lbaoTest";
//    final String uploadFilePath = testDirPath + "/uploadFile.test";
//    final String downloadFilePath = testDirPath + "/downloadFile.test";
//    final int nrBlocks = 40;
//    final int fileSize = blockSize * nrBlocks + pieceSize * 10 + 100;
//
//    public void setup(Random rand) throws IOException, HashUtil.HashBuilderException {
//        File testDir = new File(testDirPath);
//        if (testDir.exists()) {
//            testDir.delete();
//        }
//        testDir.mkdirs();
//
//        File uploadFile = new File(uploadFilePath);
//        uploadFile.createNewFile();
//        generateFile(uploadFile, rand);
//    }
//
//    private void generateFile(File file, Random rand) throws IOException {
//        FileOutputStream out = new FileOutputStream(file);
//        for (int i = 0; i < fileSize / blockSize; i++) {
//            byte[] data = new byte[blockSize];
//            rand.nextBytes(data);
//            out.write(data);
//        }
//        byte[] data = new byte[fileSize % blockSize];
//        rand.nextBytes(data);
//        out.write(data);
//        out.flush();
//        out.close();
//        LOG.info("file size expected:{} generated:{}", fileSize, file.length());
//    }
//
//    @After
//    public void cleanup() {
//        File uploadFile = new File(uploadFilePath);
//        if(uploadFile.exists()) {
//            uploadFile.delete();
//        }
//        File downFile = new File(downloadFilePath);
//        if(downFile.exists()) {
//            downFile.delete();
//        }
//        File testDir = new File(testDirPath);
//        if(testDir.exists()) {
//            testDir.delete();
//        }
//        if(testDir.exists()) {
//            Assert.assertFalse("cleanup botched", false);
//        }
//    }
//    
//    @Test
//    public void test1() throws IOException, HashUtil.HashBuilderException {
//        Random rand = new Random(1234);
//        setup(rand);
//        
//        Storage uploadStorage = StorageFactory.existingDiskMMFile(uploadFilePath);
//        Storage downloadStorage = StorageFactory.emptyDiskMMFile(downloadFilePath, fileSize);
//        LBAOFileMngr lbaoFMngr = new LBAOFileMngr(downloadStorage, IncompleteTracker.create(nrBlocks + 1), 
//                pieceSize, piecesPerBlock, 3);
//        
//    }
//}
