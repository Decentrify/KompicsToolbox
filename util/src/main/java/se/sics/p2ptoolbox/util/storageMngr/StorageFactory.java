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
import java.io.File;
import java.io.IOException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class StorageFactory {

    public static Storage getExistingFile(String pathname, int pieceSize) throws IOException {
        File file = new File(pathname);
        return new RMemMapFile(file, pieceSize);
    }
    
    public static Storage getEmptyFile(String pathname, long length, int pieceSize) throws IOException {
        File file = new File(pathname);
        if (!file.createNewFile()) {
            throw new IOException("Could not create file " + pathname);
        }
        return new RWMemMapFile(file, length, pieceSize);
    }
    
    public static PieceTracker getCompletePT(long fileLength, int pieceSize) {
        return new CompletePieceTracker(nrPieces(fileLength, pieceSize));
}
    
    public static PieceTracker getSimplePT(long fileLength, int pieceSize) {
         return new SimplePieceTracker(nrPieces(fileLength, pieceSize));
    }
    
    private static int nrPieces(long fileLength, int pieceSize) {
        return (fileLength % pieceSize == 0) ? (int)(fileLength / pieceSize) : (int)(fileLength / pieceSize + 1);
    }
    
    public static SimpleFileMngr getSimpleUploadMngr(String pathName, int pieceSize) throws IOException {
        Storage st = getExistingFile(pathName, pieceSize);
        return new SimpleFileMngr(st, getCompletePT(st.size(), pieceSize));
    }
    
    public static SimpleFileMngr getSimpleDownloadMngr(String pathName, long fileLength, int pieceSize) throws IOException {
        return new SimpleFileMngr(getEmptyFile(pathName, fileLength, pieceSize), getSimplePT(fileLength, pieceSize));
    }
    
//    public static SimpleFileMngr getBackedByteBufMngr(byte[] bytes, int pieceSize) {
//        return new SimpleFileMngr(new RWByteBuffer(bytes), getCompletePT(bytes.length, pieceSize))
//    }
}
