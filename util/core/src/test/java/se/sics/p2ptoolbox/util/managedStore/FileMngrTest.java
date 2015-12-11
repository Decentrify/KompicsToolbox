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
import se.sics.ktoolbox.util.managedStore.StorageMngrFactory;
import com.google.common.io.BaseEncoding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FileMngrTest {

    String filePath = "./src/test/resources/storageTest1/uploadFile.txt";
    int nrBlocks = 10;
    int pieceSize = 1024;
    int blockSize = 1024*1024;
    long fileSize = 10*1024*1024;

    @Before
    public void setup() throws IOException, HashUtil.HashBuilderException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
    }
    
    @After
    public void cleanup() {
        File file = new File(filePath);
        file.delete();
    }

    @Test
    public void nextBlock() throws IOException, HashUtil.HashBuilderException {
        FileMngr fileMngr = StorageMngrFactory.getIncompleteFileMngr(filePath, fileSize, blockSize, pieceSize);
        
        Assert.assertEquals((Integer)0, fileMngr.nextBlock(0, new HashSet<Integer>()));
        
        Assert.assertEquals((Integer)3, fileMngr.nextBlock(3, new HashSet<Integer>()));
    }
}
