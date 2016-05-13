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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HopsDataStorage implements Storage {

    private final String hdfsURL;
    private final String path;
    private final String yarnConf;
    private final String hdfsConf;
    private final String coreConf;

    public HopsDataStorage(String path, String endpoint, String yarnConf, String hdfsConf, String coreConf) {
        this.path = path;
        this.hdfsURL = endpoint;
        this.yarnConf = yarnConf;
        this.coreConf = coreConf;
        this.hdfsConf = hdfsConf;
    }

    private FileSystem getFileSystem() {
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser("glassfish");
        try {
            return ugi.doAs(new PrivilegedExceptionAction<FileSystem>() {
                @Override
                public FileSystem run() throws IOException {
                    Configuration conf = new Configuration();

                    File hdfs;
                    File yarn;
                    File core;

                    if (yarnConf != null && hdfsConf != null && coreConf != null) {
                        hdfs = new File(hdfsConf);
                        yarn = new File(yarnConf);
                        core = new File(coreConf);
                    } else {

                        hdfs = new File(System.getProperty("user.dir") + "/src/main/resources/hdfs-site.xml");
                        yarn = new File(System.getProperty("user.dir") + "/src/main/resources/yarn-site.xml");
                        core = new File(System.getProperty("user.dir") + "/src/main/resources/core-site.xml");
                    }

                    conf.addResource(new Path(hdfs.getAbsolutePath()));
                    conf.addResource(new Path(yarn.getAbsolutePath()));
                    conf.addResource(new Path(core.getAbsolutePath()));
                    return FileSystem.get(conf);
                }
            });
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private FileSystem testgetFileSystem() {
        try {
            File hdfs;
            File yarn;
            File core;
            Configuration conf = new Configuration();
            
            if (yarnConf != null && hdfsConf != null && coreConf != null) {
                hdfs = new File(hdfsConf);
                yarn = new File(yarnConf);
                core = new File(coreConf);
            } else {
                
                hdfs = new File(System.getProperty("user.dir") + "/src/main/resources/hdfs-site.xml");
                yarn = new File(System.getProperty("user.dir") + "/src/main/resources/yarn-site.xml");
                core = new File(System.getProperty("user.dir") + "/src/main/resources/core-site.xml");
            }
            
            conf.addResource(new Path(hdfs.getAbsolutePath()));
            conf.addResource(new Path(yarn.getAbsolutePath()));
            conf.addResource(new Path(core.getAbsolutePath()));
            return FileSystem.get(conf);
        } catch (IOException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public byte[] read(final long readPos, final int readLength) {

        byte[] byte_read = null;
        DistributedFileSystem fs = (DistributedFileSystem) this.testgetFileSystem();
        if (fs != null) {
            FSDataInputStream fdis = null;
            try {
                fdis = fs.open(new Path(path));
                fdis.readFully(byte_read, readLength, readLength);
            } catch (IOException ex) {
                Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fdis.close();
                } catch (IOException ex) {
                    Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return byte_read;

    }

    @Override
    public int write(long writePos, byte[] bytes) {
        try {
            DistributedFileSystem dfs = (DistributedFileSystem) this.getFileSystem();
            Path p = new Path(path);
            FSDataOutputStream out = dfs.create(p);
            out.write(bytes, (int) writePos, bytes.length);
            return bytes.length;
        } catch (IOException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    @Override
    public long length() {

        try {
            FileSystem fs = this.getFileSystem();

            return fs.getLength(new Path(path));

        } catch (IOException ex) {
            Logger.getLogger(HopsDataStorage.class.getName()).log(Level.SEVERE, null, ex);
        }

        return -1;
    }

}
