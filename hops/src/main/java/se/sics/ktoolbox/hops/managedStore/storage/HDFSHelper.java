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
import java.security.PrivilegedExceptionAction;
import java.util.Random;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.hops.managedStore.storage.util.HDFSResource;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSHelper {

    private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(HDFSHelper.class);
    private static String logPrefix = "";

    public static boolean canConnect(String hopsIp, int hopsPort) {
        Configuration conf = new Configuration();
        String hopsURL = hopsIp + ":" + hopsPort;
        conf.set("fs.defaultFS", hopsURL);
        LOG.debug("{}testing connection to:{}", logPrefix, hopsURL);
        try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf)) {
            LOG.debug("{}getting status from:{}", logPrefix, hopsURL);
            FsStatus status = fs.getStatus();
            LOG.debug("{}got status from:{}", logPrefix, hopsURL);
            return true;
        } catch (IOException ex) {
            LOG.warn("{}could not connect:{}", logPrefix, ex.getMessage());
            return false;
        }
    }
    
    public static Long length(String hopsURL, final String filePath, final String user) {
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            final Configuration conf = new Configuration();
            conf.set("fs.defaultFS", hopsURL);
            LOG.debug("{}getting length of file from:{}", logPrefix, hopsURL);
            long result = ugi.doAs(new PrivilegedExceptionAction<Long>() {
                public Long run() throws Exception {
                    try (FileSystem fs = FileSystem.get(conf)) {
                        long length = -1;
                        if (fs.isFile(new Path(filePath))) {
                            length = fs.getLength(new Path(filePath));
                        }
                        return length;
                    } catch (IOException ex) {
                        LOG.warn("{}could not get size of file:{}", logPrefix, ex.getMessage());
                        throw new RuntimeException(ex);
                    }
                }
            });
            LOG.debug("{}got length of file from:{}", logPrefix, hopsURL);
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not delete file:{}", logPrefix, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
    
    public static FileSystem getFileSystem(String hopsURL, final String user) {
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            final Configuration conf = new Configuration();
            conf.set("fs.defaultFS", hopsURL);
            LOG.debug("{}getting length of file from:{}", logPrefix, hopsURL);
            FileSystem result = ugi.doAs(new PrivilegedExceptionAction<FileSystem>() {
                public FileSystem run() throws Exception {
                    try {
                        FileSystem fs = FileSystem.get(conf);
                        return fs;
                    } catch (IOException ex) {
                        LOG.warn("{}could not get size of file:{}", logPrefix, ex.getMessage());
                        throw new RuntimeException(ex);
                    }
                }
            });
            LOG.debug("{}got length of file from:{}", logPrefix, hopsURL);
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not delete file:{}", logPrefix, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public static Long length(final HDFSResource resource, final String user) {
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            long result = ugi.doAs(new PrivilegedExceptionAction<Long>() {
                public Long run() throws Exception {
                    final Configuration conf = new Configuration();
                    conf.set("fs.defaultFS", resource.hopsIp + ":" + resource.hopsPort);
                    try (FileSystem fs = FileSystem.get(conf)) {
                        long length = -1;
                        if (fs.isFile(new Path(resource.dirPath + Path.SEPARATOR + resource.fileName))) {
                            length = fs.getLength(new Path(resource.dirPath + Path.SEPARATOR + resource.fileName));
                        }
                        return length;
                    } catch (IOException ex) {
                        LOG.warn("{}could not get size of file:{}", logPrefix, ex.getMessage());
                        throw new RuntimeException(ex);
                    }
                }
            });
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not delete file:{}", logPrefix, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public static boolean delete(final HDFSResource resource, final String user) {
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            boolean result = ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
                public Boolean run() throws Exception {
                    final Configuration conf = new Configuration();
                    conf.set("fs.defaultFS", "hdfs://" + resource.hopsIp + ":" + resource.hopsPort);
                    try (FileSystem fs = FileSystem.get(conf)) {
                        fs.delete(new Path(resource.dirPath + Path.SEPARATOR + resource.fileName), false);
                        return true;
                    } catch (IOException ex) {
                        LOG.warn("{}could not delete file:{}", logPrefix, ex.getMessage());
                        return false;
                    }
                }
            });
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not delete file:{}", logPrefix, ex.getMessage());
            return false;
        }
    }

    public static boolean create(final HDFSResource resource, final String user, final long fileSize) {
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            boolean result = ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws Exception {
                    Configuration conf = new Configuration();
                    conf.set("fs.defaultFS", "hdfs://" + resource.hopsIp + ":" + resource.hopsPort);
                    try (FileSystem fs = FileSystem.get(conf)) {
                        if (!fs.isDirectory(new Path(resource.dirPath))) {
                            fs.mkdirs(new Path(resource.dirPath));
                        }
                        String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
                        if (fs.isFile(new Path(filePath))) {
                            return false;
                        }
                        Random rand = new Random(1234);
                        try (FSDataOutputStream out = fs.create(new Path(filePath))) {
                            for (int i = 0; i < fileSize / 1024; i++) {
                                byte[] data = new byte[1024];
                                rand.nextBytes(data);
                                out.write(data);
                                out.flush();
                            }
                            if (fileSize % 1024 != 0) {
                                byte[] data = new byte[(int) (fileSize % 1024)];
                                rand.nextBytes(data);
                                out.write(data);
                                out.flush();
                            }
                            return true;
                        } catch (IOException ex) {
                            LOG.warn("{}could not write file:{}", logPrefix, ex.getMessage());
                            return false;
                        }
                    } catch (IOException ex) {
                        LOG.warn("{}could not create file:{}", logPrefix, ex.getMessage());
                        return false;
                    }
                }
            });
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not create file:{}", logPrefix, ex.getMessage());
            return false;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] read(final HDFSResource resource, String user, final long readPos, final int readLength) throws IOException, InterruptedException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs");
        String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf);
                FSDataInputStream in = fs.open(new Path(filePath))) {
            UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
            byte[] result = ugi.doAs(new PrivilegedExceptionAction<byte[]>() {
                @Override
                public byte[] run() throws Exception {
                    byte[] byte_read = new byte[readLength];
                    in.readFully(readPos, byte_read);
                    return byte_read;
                }
            });
            return result;
        }
    }

    public static int append(final HDFSResource resource, String user, final byte[] data) throws IOException, InterruptedException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs");
        String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf);
                FSDataOutputStream out = fs.append(new Path(filePath))) {
            UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
            int result = ugi.doAs(new PrivilegedExceptionAction<Integer>() {
                @Override
                public Integer run() throws Exception {
                    out.write(data);
                    out.flush();
                    return data.length;
                }
            });
            return result;
        }
    }
}
