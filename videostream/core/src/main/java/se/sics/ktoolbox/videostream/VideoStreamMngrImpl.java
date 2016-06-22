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
package se.sics.ktoolbox.videostream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VideoStreamMngrImpl implements VideoStreamManager {

    private static final Logger LOG = LoggerFactory.getLogger(VideoStreamMngrImpl.class);
    private static final int piecesToSend = 1000;
    private static final int waitTime = 20;
    private final FileMngr fm;
    private final int pieceSize;
    private final long fileSize;
    private final AtomicInteger playPos;

    public VideoStreamMngrImpl(FileMngr fm, int pieceSize, long fileSize, AtomicInteger playPos) {
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
        LOG.debug("getting content from readPos:{} to endPos:{}", readPos, endPos);
        ByteBuf buf = Unpooled.buffer();

        int pieceIdx = (int) (readPos / pieceSize);
        int startOffset = (int) (readPos % pieceSize);

        int endPieceIdx = (int) (endPos / pieceSize);
        int endOffset = (int) (endPos % pieceSize);
        if (endOffset == 0) {
            endPieceIdx--;
        }

        int restToSend = piecesToSend;
        if (!fm.hasPiece(pieceIdx)) {
            playPos.set(pieceIdx);
        }

        int currWaitTime = 0;
        while (!fm.hasPiece(pieceIdx)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOG.error("error while waiting for pieces");
                System.exit(1);
            }
            currWaitTime++;
            if (currWaitTime > waitTime) {
                LOG.warn("problem retrieving pieces");
                return new byte[]{};
            }
        }
        while (fm.hasPiece(pieceIdx) && pieceIdx < endPieceIdx && restToSend > 0) {
            ByteBuffer piece = fm.readPiece(null, pieceIdx, (Set)new HashSet<Integer>());
            buf.writeBytes(piece.array(), startOffset, piece.array().length - startOffset);
            LOG.debug("sending piece:{} total:{}", new Object[]{pieceIdx, buf.writerIndex()});
            pieceIdx++;
            restToSend--;
            startOffset = 0;
        }
        if (pieceIdx == endPieceIdx && readPos / pieceSize == pieceIdx) { //if start piece = end piece
            if (fm.hasPiece(pieceIdx)) {
                if (endOffset == 0) {
                    ByteBuffer piece = fm.readPiece(null, pieceIdx, null);
                    int pieceStartOffset = startOffset;
                    int pieceLength = piece.array().length - pieceStartOffset;
                    buf.writeBytes(piece.array(), pieceStartOffset, piece.array().length);
                    LOG.debug("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                } else {
                    ByteBuffer piece = fm.readPiece(null, pieceIdx, null);
                    int pieceStartOffset = startOffset;
                    int pieceLength = (endOffset < piece.array().length ? endOffset + 1 : piece.array().length) - startOffset;
                    buf.writeBytes(piece.array(), pieceStartOffset, pieceLength);
                    LOG.debug("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                }
            }
        } else if (pieceIdx == endPieceIdx) { //if need to write part of end piece
            if (fm.hasPiece(pieceIdx)) {
                if (endOffset == 0) {
                    ByteBuffer piece = fm.readPiece(null, pieceIdx, null);
                    int pieceStartOffset = 0;
                    int pieceLength = piece.array().length;
                    buf.writeBytes(piece);
                    LOG.debug("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                } else {
                    ByteBuffer piece = fm.readPiece(null, pieceIdx, null);
                    int pieceStartOffset = 0;
                    int pieceLength = (endOffset < piece.array().length ? endOffset + 1 : piece.array().length);
                    buf.writeBytes(piece.array(), pieceStartOffset, pieceLength);
                    LOG.debug("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
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
