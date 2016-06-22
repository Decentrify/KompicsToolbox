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

package se.sics.ktoolbox.util.managedStore.core.impl.block;

import se.sics.ktoolbox.util.managedStore.core.BlockMngr;
import se.sics.ktoolbox.util.managedStore.core.Storage;
import se.sics.ktoolbox.util.managedStore.core.ComponentTracker;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class InMemoryBlockMngr implements BlockMngr {
    private final Storage storage;
    private final ComponentTracker tracker;
    private final int blockSize;
    private final int pieceSize;
    private final int lastPiece;
    private final int lastPieceSize;
    
    public InMemoryBlockMngr(Storage storage, ComponentTracker pieceTracker, int blockSize, int pieceSize) {
        this.storage = storage;
        this.tracker = pieceTracker;
        this.blockSize = blockSize;
        this.pieceSize = pieceSize;
        this.lastPiece = (blockSize % pieceSize == 0) ? blockSize / pieceSize - 1 : blockSize / pieceSize;
        this.lastPieceSize = (blockSize % pieceSize == 0) ? pieceSize : blockSize % pieceSize;
    }

    @Override
    public boolean hasPiece(int pieceNr) {
        return tracker.hasComponent(pieceNr);
    }

    @Override
    public int writePiece(int pieceNr, byte[] piece) {
        tracker.addComponent(pieceNr);
        long writePos = pieceNr * pieceSize;
        return storage.write(writePos, piece);
    }

    @Override
    public boolean isComplete() {
        return tracker.isComplete(0);
    }

    @Override
    public byte[] getBlock() {
        return storage.read(null, 0, blockSize, null);
    }

    @Override
    public int nrPieces() {
        return lastPiece + 1;
    }
}
