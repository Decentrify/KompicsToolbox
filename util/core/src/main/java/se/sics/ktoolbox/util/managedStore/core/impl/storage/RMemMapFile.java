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
package se.sics.ktoolbox.util.managedStore.core.impl.storage;

import se.sics.ktoolbox.util.managedStore.core.Storage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RMemMapFile implements Storage {

    private final long length;
    private final MappedByteBuffer mbb;

    RMemMapFile(File file) throws IOException {
        this.length = file.length();
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        mbb = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
        raf.close();
    }
    
    @Override
    public void tearDown() {
    }
    
    @Override
    public byte[] read(long readPos, int readLength) {
        if(readPos > Integer.MAX_VALUE) {
            throw new RuntimeException("MemoryMappedFiles only allow integer size for read values");
        }
        if(readPos > length) {
            return null;
        }
        if(readPos + readLength > length) {
            throw new RuntimeException("not tested yet");
//            readLength = (int)(length - readPos);
        }
        return read((int)readPos, (int)readLength);
    }
    
    private byte[] read(int readPos, int readLength) {
        byte[] result = new byte[readLength];
        mbb.position(readPos);
        mbb.get(result, 0, result.length);
        return result;
    }

    @Override
    public int write(long writePos, byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long length() {
        return length;
    }
}
