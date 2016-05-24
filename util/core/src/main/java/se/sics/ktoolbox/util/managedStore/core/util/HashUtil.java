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
package se.sics.ktoolbox.util.managedStore.core.util;

import com.google.common.io.BaseEncoding;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HashUtil {

    public static final byte SHA = 0x01;

    private static final Map<String, Byte> nameMap = new HashMap<String, Byte>();
    private static final Map<Byte, Pair<String, Integer>> algMap = new HashMap<Byte, Pair<String, Integer>>();

    static {
        nameMap.put("SHA", SHA);
        algMap.put(SHA, Pair.with("SHA", 20));
    }

    public static int getHashSize(String hashAlg) {
        if (!nameMap.containsKey(hashAlg)) {
            throw new RuntimeException("hash algorithm: " + hashAlg + " not defined");
        }
        return algMap.get(nameMap.get(hashAlg)).getValue1();
    }

    public static byte getAlgId(String hashAlg) {
        return nameMap.get(hashAlg);
    }

    public static String getAlgName(byte algId) {
        return algMap.get(algId).getValue0();
    }
    
    public static byte[] makeHash(byte[] bytes, String hashAlg) {
        try {
            MessageDigest md = MessageDigest.getInstance(hashAlg);
            return md.digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void makeHashes(String filePath, String hashFilePath, String hashAlg, int pieceSize) throws HashBuilderException {
        OutputStream hashWriter = null;
        InputStream fileReader = null;
        try {
            hashWriter = new FileOutputStream(hashFilePath);
            fileReader = new FileInputStream(filePath);
            MessageDigest md = MessageDigest.getInstance(hashAlg);
            while (fileReader.available() >= pieceSize) {
                byte[] piece = new byte[pieceSize];
                fileReader.read(piece);
                hashWriter.write(md.digest(piece));
            }
            int rest = fileReader.available();
            if (rest != 0) {
                byte[] piece = new byte[rest];
                fileReader.read(piece);
                hashWriter.write(md.digest(piece));
            }
            hashWriter.flush();
            fileReader.close();
        } catch (FileNotFoundException ex) {
            throw new HashBuilderException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new HashBuilderException(ex);
        } catch (IOException ex) {
            throw new HashBuilderException(ex);
        } finally {
            try {
                if (hashWriter != null) {
                    hashWriter.close();
                }
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException ex) {
                throw new HashBuilderException(ex);
            }
        }
    }

    public static boolean checkHash(String hashAlg, byte[] piece, byte[] hash) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(hashAlg);
        } catch (NoSuchAlgorithmException ex) {
            System.exit(1);
            throw new RuntimeException(ex);
        }
        return Arrays.equals(hash, md.digest(piece));
    }

    public static boolean checkHash(byte hashId, byte[] piece, byte[] hash) {
        return checkHash(algMap.get(hashId).getValue0(), piece, hash);
    }

    public static class HashBuilderException extends Exception {

        public HashBuilderException(Throwable cause) {
            super(cause);
        }
    }
}
