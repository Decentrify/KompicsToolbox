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

import java.util.Set;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimpleFileMngr implements FileMngr {

    private final Storage file;
    private final PieceTracker pieceTracker;
    
    
    public SimpleFileMngr(Storage file, PieceTracker pieces) {
        this.file = file;
        this.pieceTracker = pieces;
    }

    @Override
    public boolean isComplete() {
        return pieceTracker.isComplete();
    }

    @Override
    public Set<Integer> nextPiecesNeeded(int n, int startPos) {
        return pieceTracker.nextPiecesNeeded(n, startPos);
    }
    
    @Override
    public boolean hasPiece(int piecePos) {
        return pieceTracker.hasPiece(piecePos);
    }

    @Override
    public byte[] readPiece(int piecePos) {
        return file.readPiece(piecePos);
    }

    @Override
    public void writePiece(int piecePos, byte[] piece) {
        pieceTracker.addPiece(piecePos);
        file.writePiece(piecePos, piece);
    }

    @Override
    public int contiguousStart() {
        return pieceTracker.contiguousStart();
    }
    
    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public long size() {
        return file.size();
    }
}
