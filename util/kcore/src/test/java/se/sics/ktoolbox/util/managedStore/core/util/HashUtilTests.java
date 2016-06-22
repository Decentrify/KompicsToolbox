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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import se.sics.ktoolbox.util.profiling.KProfiler;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HashUtilTests {
//    @Test
    public void profileHashing() {
        Random rand = new Random(123);
        byte[] block;
        String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
        
        KProfiler kp = new KProfiler(KProfiler.Type.LOG);
        
        for(int i = 0; i < 1000; i++) {
            block = new byte[1024*1024];
            kp.start("HashUtil", "hashing");
            HashUtil.makeHash(block, hashAlg);
            kp.end();
        }
    }
    
//    @Test
    public void profileRawHashing() throws NoSuchAlgorithmException {
        Random rand = new Random(123);
        byte[] block;
        String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
        MessageDigest md = MessageDigest.getInstance(hashAlg);
        
        KProfiler kp = new KProfiler(KProfiler.Type.LOG);
        
        for(int i = 0; i < 1000; i++) {
            block = new byte[1024*1024];
            kp.start("HashUtil", "hashing");
            md.digest(block);
            kp.end();
        }
    }
    
    @Test
    public void profileHashCheck() {
        Random rand = new Random(123);
        byte[] block, hash;
        String hashAlg = HashUtil.getAlgName(HashUtil.SHA);
        KProfiler kp = new KProfiler(KProfiler.Type.LOG);
        
        for(int i = 0; i < 1000; i++) {
            block = new byte[1024*1024];
            hash = HashUtil.makeHash(block, hashAlg);
            kp.start("HashUtil", "hashCheck");
            boolean res = HashUtil.checkHash(hashAlg, block, hash);
            kp.end();
            Assert.assertTrue(res);
        }
    }
}
