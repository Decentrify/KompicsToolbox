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

import java.net.URI;
import se.sics.ktoolbox.util.managedStore.core.Storage;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSClient;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HopsDataStorage implements Storage {

    private static final String HDFS_URL = "localhost:9000";
    private String remoteFilename;

    public String getRemoteFilename() {
        return remoteFilename;
    }

    public void setRemoteFilename(String remoteFilename) {
        this.remoteFilename = remoteFilename;
    }
    
    @Override
    public byte[] read(long readPos, int readLength) {
        
        Configuration conf = new Configuration();
        conf.set("fs.defaulFS", HDFS_URL);
        DFSClient client = null;
        BufferedInputStream input = null;
        byte[] read_bytes = null;
        try {
            client = new DFSClient(new URI(HDFS_URL), conf);
            input = new BufferedInputStream(client.open(remoteFilename));
            input.read(read_bytes, (int)readPos, readLength);
            
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return read_bytes;
    }

    @Override
    public int write(long writePos, byte[] bytes) {
        
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", HDFS_URL);
        DFSClient client = null;
        BufferedOutputStream out = null;
        try {
            client = new DFSClient(new URI(HDFS_URL), conf);
            out = new BufferedOutputStream(client.create(remoteFilename, true));
            out.write(bytes, (int) writePos, bytes.length);
            return bytes.length;
            
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return -1;
    }

    @Override
    public long length() {
        
        Configuration conf = new Configuration();
        conf.set("fs.defaulFS", HDFS_URL);
        
        DFSClient client = null;
        try {
            client = new DFSClient(new URI(HDFS_URL), conf);
            return client.open(remoteFilename).getFileLength();
            
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return -1;
    }
}
