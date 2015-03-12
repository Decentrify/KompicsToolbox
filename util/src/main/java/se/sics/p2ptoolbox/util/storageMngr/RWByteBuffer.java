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

import io.netty.buffer.ByteBuf;
import java.io.IOException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RWByteBuffer implements Storage {
    private final int bufLength;
    private final ByteBuf buffer;
    private final int pieceSize;
    private final int lastPiece;

    RWByteBuffer(ByteBuf buffer, int pieceSize) throws IOException {
        this.buffer = buffer;
        this.bufLength = buffer.array().length;
        this.pieceSize = pieceSize;
        this.lastPiece = (bufLength % pieceSize == 0) ? (int) (bufLength / pieceSize) - 1 : (int) (bufLength / pieceSize);
    }
    
    @Override
    public int nrPieces() {
        return lastPiece + 1;
    }

    @Override
    public byte[] readPiece(int piecePos) {
        byte[] result;
        if (lastPiece == piecePos) {
            if (bufLength % pieceSize == 0) {
                result = new byte[pieceSize];
            } else {
                result = new byte[(int) (bufLength % pieceSize)];
            }
        } else {
            result = new byte[pieceSize];
        }
        int readStart = piecePos * pieceSize;
        buffer.readerIndex(readStart);
        buffer.readBytes(result);
        return result;
    }

    @Override
    public void writePiece(int piecePos, byte[] piece) {
        int writeStart = piecePos * pieceSize;
        int auxWriterIndex = buffer.writerIndex();
        buffer.writerIndex(writeStart);
        long restFile = bufLength - writeStart;
        int writeBytes = (piece.length < restFile ? piece.length : (int)restFile);
        buffer.writeBytes(piece, 0, writeBytes);
        int auxWriterIndex2 = buffer.writerIndex();
        if(auxWriterIndex2 < auxWriterIndex) {
            buffer.writerIndex(auxWriterIndex);
        }
    }
    
    @Override
    public String toString() {
        return "ByteBuffer";
    }

    @Override
    public long size() {
        return bufLength;
    }
}
