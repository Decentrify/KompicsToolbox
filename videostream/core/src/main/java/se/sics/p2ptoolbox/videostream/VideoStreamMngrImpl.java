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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.storageMngr.FileMngr;
import se.sics.p2ptoolbox.util.storageMngr.StorageFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VideoStreamMngrImpl implements VideoStreamManager {

    private static final Logger log = LoggerFactory.getLogger(VideoStreamMngrImpl.class);
    private static final int piecesToSend = 5;
    private final FileMngr fm;
    private final int blockSize;

    public VideoStreamMngrImpl(String videoPath, int readBlockSize) throws IOException {
        this.fm = StorageFactory.getSimpleUploadMngr(videoPath, readBlockSize);
        this.blockSize = readBlockSize;
    }

    @Override
    public long getLength() {
        return fm.size();
    }

    @Override
    public synchronized byte[] getContent(long readPos) {
        return getContent(readPos, readPos + piecesToSend * blockSize);
    }

    @Override
    public synchronized byte[] getContent(long readPos, long endPos) {
        ByteBuf buf = Unpooled.buffer(blockSize);

        int pieceIdx = (int) (readPos / blockSize);
        int startOffset = (int) (readPos % blockSize);

        int endPieceIdx = (int) (endPos / blockSize);
        int endOffset = (int) (endPos % blockSize);
        if (endOffset == 0) {
            endPieceIdx--;
        }

        int restToSend = piecesToSend;
        while (fm.hasPiece(pieceIdx) && pieceIdx < endPieceIdx && restToSend > 0) {
            byte[] piece = fm.readPiece(pieceIdx);
            buf.writeBytes(piece, startOffset, piece.length - startOffset);
            log.info("sending piece:{} total:{}", new Object[]{pieceIdx, piece.length, buf.writerIndex()});
            pieceIdx++;
            restToSend--;
            startOffset = 0;
        }
        if (pieceIdx == endPieceIdx && readPos / blockSize == pieceIdx) { //if start piece = end piece
            if (fm.hasPiece(pieceIdx)) {
                if (endOffset == 0) {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = startOffset;
                    int pieceLength = piece.length - pieceStartOffset;
                    buf.writeBytes(piece, pieceStartOffset, piece.length);
                    log.info("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                } else {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = startOffset;
                    int pieceLength = (endOffset < piece.length ? endOffset + 1 : piece.length) - startOffset;
                    buf.writeBytes(piece, pieceStartOffset, pieceLength);
                    log.info("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                }
            }
        } else if (pieceIdx == endPieceIdx) { //if need to write part of end piece
            if (fm.hasPiece(pieceIdx)) {
                if (endOffset == 0) {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = 0;
                    int pieceLength = piece.length;
                    buf.writeBytes(piece);
                    log.info("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                } else {
                    byte[] piece = fm.readPiece(pieceIdx);
                    int pieceStartOffset = 0;
                    int pieceLength = (endOffset < piece.length ? endOffset + 1 : piece.length);
                    buf.writeBytes(fm.readPiece(pieceIdx), pieceStartOffset, pieceLength);
                    log.info("sending part of piece:{} startOff:{} endOff:{} total: {}", new Object[]{pieceIdx, pieceStartOffset, pieceLength, buf.writerIndex()});
                }
            }
        }
        byte[] ret = new byte[buf.writerIndex()];
        System.arraycopy(buf.array(), 0, ret, 0, buf.writerIndex());
        return ret;
    }
}
