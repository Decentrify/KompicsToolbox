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
package se.sics.ktoolbox.util.stream.transfer;

import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;
import se.sics.ktoolbox.util.stream.ranges.KBlock;
import se.sics.ktoolbox.util.stream.ranges.KBlockImpl;
import se.sics.ktoolbox.util.stream.ranges.KPiece;
import se.sics.ktoolbox.util.stream.ranges.KPieceImpl;
import se.sics.ktoolbox.util.stream.util.BlockDetails;
import se.sics.ktoolbox.util.stream.util.FileDetails;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BlockHelper {
    public static int getBlockNr(long pieceNr, FileDetails fileDetails) {
        int blockNr = (int)pieceNr / fileDetails.defaultBlock.nrPieces;
        return blockNr;
    }
    
    public static int getBlockPieceNr(long pieceNr, FileDetails fileDetails) {
        int bpNr = (int)pieceNr % fileDetails.defaultBlock.nrPieces;
        return bpNr;
    }
    
    public static KBlock getBlockRange(int blockNr, FileDetails fileDetails) {
        BlockDetails blockDetails = fileDetails.getBlockDetails(blockNr);
        long lower = blockNr * fileDetails.defaultBlock.blockSize;
        long higher = lower + blockDetails.blockSize;
        return new KBlockImpl(blockNr, lower, higher);
    }

    public static KPiece getPieceRange(long pieceNr, FileDetails fileDetails) {
        int blockNr = getBlockNr(pieceNr, fileDetails);
        int bpNr = getBlockPieceNr(pieceNr, fileDetails);
        BlockDetails blockDetails = fileDetails.getBlockDetails(blockNr);
        int pieceSize = blockDetails.getPieceSize(bpNr);
        long lower = blockNr * fileDetails.defaultBlock.blockSize + bpNr * blockDetails.defaultPieceSize;
        long higher = lower + pieceSize;
        return new KPieceImpl(blockNr, pieceNr, bpNr, lower, higher);
    }

    public static KBlock getHashRange(int blockNr, FileDetails fileDetails) {
        int hashSize = HashUtil.getHashSize(fileDetails.hashAlg);
        long lower = blockNr * hashSize;
        long higher = lower + hashSize;
        return new KBlockImpl(blockNr, lower, higher);
    }
}
