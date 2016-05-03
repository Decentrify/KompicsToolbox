/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.hops.managedStore.storage;


import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HopsDataStorage implements Storage {

    private static final String HDFS_URL = "hdfs://10.0.2.15:8023";
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    
    
    @Override
    public byte[] read(long readPos, int readLength) {
        
        byte [] byte_read = null;
        
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", HDFS_URL);
        
        try {
            FileSystem fs = FileSystem.get(conf);
            FSDataInputStream inputStream = fs.open(new Path(path));
            
            inputStream.read(byte_read, (int)readPos, readLength);
            
            
        } catch (IOException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return byte_read;
    }

    @Override
    public int write(long writePos, byte[] bytes) {
        
        Configuration conf = new Configuration();
        conf.set("fs.default", HDFS_URL);
        
        try {
            FileSystem fs = FileSystem.get(conf);
            FSDataOutputStream out = fs.append(new Path(path));
            
            out.write(bytes, (int) writePos, bytes.length);
            
            return bytes.length;
            
        } catch (IOException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return -1;
        
    }

    @Override
    public long length() {
        
        Configuration conf = new Configuration();
        conf.set("fs.default", HDFS_URL);
        
        try {
            FileSystem fs = FileSystem.get(conf);
            
            return fs.getLength(new Path(path));
            
        } catch (IOException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        return -1;
    }
    
}
