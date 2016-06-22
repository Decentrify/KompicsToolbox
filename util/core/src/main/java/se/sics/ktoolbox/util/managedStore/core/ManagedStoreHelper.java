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
package se.sics.ktoolbox.util.managedStore.core;

import org.javatuples.Pair;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ManagedStoreHelper {
    public static long MAX_BYTE_FILE_SIZE = (long)Integer.MAX_VALUE * Integer.MAX_VALUE;
    //TODO Alex - do some tests to see what BitSet can do properly - this is arbitrarily set
    public static int MAX_BIT_SET_SIZE = 1024*1024;
    public static int MAX_BLOCKS = MAX_BIT_SET_SIZE;
    public static int MAX_PIECES_PER_BLOCK = 1024;
    public static int DEFAULT_PIECE_SIZE = 1024;
    static {
        assert MAX_PIECES_PER_BLOCK * MAX_BLOCKS <= 1024 * 1024 * 1024; //this is less that Integer.Max_Value and 1GB size limit on blocks
    }
    
    public static int nrComponents(long storageSize, int componentSize) {
        if(storageSize == 0) {
            return 0;
        }
        return storageSize % componentSize == 0 ? (int)(storageSize / componentSize) : (int)(storageSize / componentSize + 1);
    }
    
    public static int componentNr(long absolutePos, int componentSize) {
        return (int)absolutePos / componentSize;
    }
    
    /**
     * @param totalSize
     * @param componentSize
     * @return <lastComponentNr, lastComponentSize>
     */
    public static Pair<Integer, Integer> lastComponent(long storeSize, int componentSize) {
        int lastComponentNr = (storeSize % componentSize == 0) ? (int) (storeSize / componentSize) - 1 : (int) (storeSize / componentSize);
        int lastComponentSize = (storeSize % componentSize == 0) ? componentSize : (int) (storeSize % componentSize);
        return Pair.with(lastComponentNr, lastComponentSize);
    }
    
    /**
     * Example - subComp - piece, comp - block - translate pieceNr to <blockNr, inBlockPos>
     * Example - subcomp - absPosition, comp - piece - translate absolutePosition to <pieceNr, inPiecePos>
     * @param subCompPosition
     * @param subCompPerComp
     */
    public static Pair<Integer, Integer> componentDetails(long subCompPosition, int subCompPerComp) {
        int blockNr = componentNr(subCompPosition, subCompPerComp);
        int inBlockNr = (int)(subCompPosition % subCompPerComp);
        return Pair.with(blockNr, inBlockNr);
    }
    
    /**
     * @return <<pieceNr, pieceOffset>, <blockNr, blockOffset>>
     */
    public static Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> blockDetails(long position, int piecesPerBlock, int pieceSize) {
        int pieceNr = componentNr(position, pieceSize);
        return Pair.with(componentDetails(position, pieceSize), componentDetails(pieceNr, piecesPerBlock));
    }
    
    public static int blockNr(long position, int piecesPerBlock, int pieceSize) {
        return blockDetails(position, piecesPerBlock, pieceSize).getValue1().getValue0();
    }
    
    public static int blockSize(int blockNr, long fileSize, int piecesPerBlock, int pieceSize) {
        Pair<Integer, Integer> lastComponent = lastComponent(fileSize, piecesPerBlock * pieceSize);
        if(lastComponent.getValue0().equals(blockNr)) {
            return lastComponent.getValue1();
        } else {
            return piecesPerBlock * pieceSize;
        }
    }
    
    public static Pair<Integer, Integer> lastBlock(long fileSize, int blockSize) {
        return lastComponent(fileSize, blockSize);
    }
}
