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

package se.sics.ktoolbox.util.managedStore.core;

import java.nio.ByteBuffer;
import java.util.Set;
import se.sics.kompics.id.Identifier;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface FileMngr {
    public void tearDown();
    
    //absolute position
    public boolean has(long readPos, int length);
    public ByteBuffer read(Identifier readerId, long readPos, int length, Set<Integer> bufferBlocks);
    //piece position
    public boolean hasPiece(int pieceNr);
    public ByteBuffer readPiece(Identifier readerId, int pieceNr, Set<Integer> bufferBlocks);
    //block position
    public double percentageCompleted();
    public boolean isComplete(int fromBlockNr);
    public int contiguous(int fromBlockNr);
    
    public int writeBlock(int blockNr, ByteBuffer block);
    public Integer nextBlock(int blockNr, Set<Integer> exclude);
    public int blockSize(int blockNr);
    
    public long length();
}
