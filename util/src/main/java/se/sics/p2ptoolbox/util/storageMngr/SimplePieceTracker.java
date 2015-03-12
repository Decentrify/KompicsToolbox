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

package se.sics.p2ptoolbox.util.storageMngr;

import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimplePieceTracker implements PieceTracker {
    private final BitSet pieces;
    private final int nrPieces;
    
    public SimplePieceTracker(int nrPieces) {
        this.pieces = new BitSet(nrPieces+1);
        this.nrPieces = nrPieces;
    }
    
    @Override
    public boolean isComplete() {
        return pieces.nextClearBit(0) == nrPieces;
    }
    
    @Override
    public boolean hasPiece(int piecePos) {
        return pieces.get(piecePos);
    }
    
    @Override
    public Set<Integer> nextPiecesNeeded(int n, int startPos) {
        Set<Integer> result = new TreeSet<Integer>();
        int nextPos = startPos;
        while(result.size() < n) {
            nextPos = pieces.nextClearBit(nextPos);
            if(nextPos < nrPieces) {
                result.add(nextPos);
                nextPos++;
            } else {
                break;
            }
        }
        return result;
    }
    
    @Override
    public void addPiece(int piecePos) {
        pieces.set(piecePos);
    }
    
    @Override
    public int contiguousStart() {
        int nextClear = pieces.nextClearBit(0);
        return nextClear;
    }
}
