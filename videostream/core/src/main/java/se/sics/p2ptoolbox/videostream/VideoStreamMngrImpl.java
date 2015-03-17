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
package se.sics.p2ptoolbox.videostream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.managedStore.FileMngr;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VideoStreamMngrImpl implements VideoStreamManager {

    private static final Logger log = LoggerFactory.getLogger(VideoStreamMngrImpl.class);
    private static final int piecesToSend = 5*1000;
    private final FileMngr fm;
    private final int pieceSize;
    private final long fileSize;
    private final AtomicInteger playPos;

    public VideoStreamMngrImpl(FileMngr fm, int pieceSize, long fileSize, AtomicInteger playPos) throws IOException {
        this.fm = fm;
        this.pieceSize = pieceSize;
        this.fileSize = fileSize;
        this.playPos = playPos;
    }

    @Override
    public long getLength() {
        return fileSize;
    }

    @Override
    public synchronized byte[] getContent(long readPos) {
        return getContent(readPos, readPos + piecesToSend * pieceSize);
    }

    @Override
    public synchronized byte[] getContent(long readPos, long endPos) {
        ByteBuf buf = Unpooled.buffer();

        int pieceIdx = (int) (readPos / pieceSize);
        int startOffset = (int) (readPos % pieceSize);

        int endPieceIdx = (int) (endPos / pieceSize);
        int endOffset = (int) (endPos % pieceSize);
        if (endOffset == 0) {
            endPieceIdx--;
        }

        int restToSend = piecesToSend;
        if(!fm.hasPiece(pieceIdx)) {
            playPos.set(pieceIdx);
        }
        while(!fm.hasPiece(pieceIdx)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                log.error("error while waiting for pieces");
                System.exit(1);
            }
        }
        while (fm.hasPiece(pieceIdx) && pieceIdx < endPieceIdx && restToSend > 0) {
            byte[] piece = fm.readPiece(pieceIdx);
            buf.writeBytes(piece, startOffset, piece.length - startOffset);
            log.trace("sending piece:{} total:{}", new Object[]{pieceIdx, buf.writerIndex()});
            pieceIdx++;
            restToSend--;
            startOffset = 0;
        }
        if (pieceIdx == endPieceIdx && readPos / pieceSize == pieceIdx) { //if start piece = end piece
            if (fm.hasPiece(pieceIdx)) {
                if (endOffset == 0) {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = startOffset;
                    int pieceLength = piece.length - pieceStartOffset;
                    buf.writeBytes(piece, pieceStartOffset, piece.length);
                    log.trace("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                } else {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = startOffset;
                    int pieceLength = (endOffset < piece.length ? endOffset + 1 : piece.length) - startOffset;
                    buf.writeBytes(piece, pieceStartOffset, pieceLength);
                    log.trace("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                }
            }
        } else if (pieceIdx == endPieceIdx) { //if need to write part of end piece
            if (fm.hasPiece(pieceIdx)) {
                if (endOffset == 0) {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = 0;
                    int pieceLength = piece.length;
                    buf.writeBytes(piece);
                    log.trace("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                } else {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = 0;
                    int pieceLength = (endOffset < piece.length ? endOffset + 1 : piece.length);
                    buf.writeBytes(fm.readPiece(pieceIdx), pieceStartOffset, pieceLength);
                    log.trace("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                }
            }
        }
        byte[] ret = new byte[buf.writerIndex()];
        System.arraycopy(buf.array(), 0, ret, 0, buf.writerIndex());
        return ret;
    }

    public void stop() {
        playPos.set(0);
    }
    
}
