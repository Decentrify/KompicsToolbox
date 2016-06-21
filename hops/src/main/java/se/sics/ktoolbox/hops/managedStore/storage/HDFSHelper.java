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
        String hopsURL = "hdfs://" + hopsIp + ":" + hopsPort;
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
        } catch (Exception ex) {
            LOG.error("{}unexpected exception:{}", logPrefix, ex);
            throw new RuntimeException(ex);
        }
    }

    public static Long length(HDFSResource resource, String user) {
        final Configuration conf = new Configuration();
        String hopsURL = "hdfs://" + resource.hopsIp + ":" + resource.hopsPort;
        conf.set("fs.defaultFS", hopsURL);
        final String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        LOG.debug("{}getting length of file:{} from:{}", new Object[]{logPrefix, filePath, hopsURL});

        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
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
        } catch (Exception ex) {
            LOG.error("{}unexpected exception:{}", logPrefix, ex);
            throw new RuntimeException(ex);
        }
    }

    public static boolean delete(HDFSResource resource, String user) {
        final Configuration conf = new Configuration();
        String hopsURL = "hdfs://" + resource.hopsIp + ":" + resource.hopsPort;
        conf.set("fs.defaultFS", hopsURL);
        final String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        LOG.debug("{}deleting file:{} from:{}", new Object[]{logPrefix, filePath, hopsURL});
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            boolean result = ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
                public Boolean run() throws Exception {
                    try (FileSystem fs = FileSystem.get(conf)) {
                        fs.delete(new Path(filePath), false);
                        return true;
                    } catch (IOException ex) {
                        LOG.warn("{}could not delete file:{}", logPrefix, ex.getMessage());
                        return false;
                    }
                }
            });
            LOG.debug("{}deleted file from:{}", logPrefix, hopsURL);
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not delete file:{}", logPrefix, ex.getMessage());
            return false;
        } catch (Exception ex) {
            LOG.error("{}unexpected exception:{}", logPrefix, ex);
            throw new RuntimeException(ex);
        }
    }

    public static boolean simpleCreate(final HDFSResource resource, String user) {
        final Configuration conf = new Configuration();
        String hopsURL = "hdfs://" + resource.hopsIp + ":" + resource.hopsPort;
        conf.set("fs.defaultFS", hopsURL);
        final String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        LOG.debug("{}creating file:{} on:{}", new Object[]{logPrefix, filePath, hopsURL});
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            boolean result = ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws Exception {
                    try (FileSystem fs = FileSystem.get(conf)) {
                        if (!fs.isDirectory(new Path(resource.dirPath))) {
                            return false;
                        }
                        if (fs.isFile(new Path(filePath))) {
                            return false;
                        }
                        try (FSDataOutputStream out = fs.create(new Path(filePath), (short)1)) {
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
            LOG.debug("{}created file from:{}", logPrefix, hopsURL);
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not create file:{}", logPrefix, ex.getMessage());
            return false;
        } catch (Exception ex) {
            LOG.error("{}unexpected exception:{}", logPrefix, ex);
            throw new RuntimeException(ex);
        }
    }

    public static boolean create(final HDFSResource resource, String user, final long fileSize) {
        final Configuration conf = new Configuration();
        String hopsURL = "hdfs://" + resource.hopsIp + ":" + resource.hopsPort;
        conf.set("fs.defaultFS", hopsURL);
        final String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        LOG.debug("{}creating file:{} on:{}", new Object[]{logPrefix, filePath, hopsURL});
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            boolean result = ugi.doAs(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws Exception {
                    try (FileSystem fs = FileSystem.get(conf)) {
                        if (!fs.isDirectory(new Path(resource.dirPath))) {
                            fs.mkdirs(new Path(resource.dirPath));
                        }
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
            LOG.debug("{}created file from:{}", logPrefix, hopsURL);
            return result;
        } catch (IOException | InterruptedException ex) {
            LOG.warn("{}could not create file:{}", logPrefix, ex.getMessage());
            return false;
        } catch (Exception ex) {
            LOG.error("{}unexpected exception:{}", logPrefix, ex);
            throw new RuntimeException(ex);
        }
    }

    public static byte[] read(HDFSResource resource, String user, final long readPos, final int readLength) throws IOException, InterruptedException {
        final Configuration conf = new Configuration();
        String hopsURL = "hdfs://" + resource.hopsIp + ":" + resource.hopsPort;
        conf.set("fs.defaultFS", hopsURL);
        final String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        LOG.debug("{}reading from file:{} from:{}", new Object[]{logPrefix, filePath, hopsURL});
        try {
            UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
            byte[] result = ugi.doAs(new PrivilegedExceptionAction<byte[]>() {
                @Override
                public byte[] run() throws Exception {
                    try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf);
                            FSDataInputStream in = fs.open(new Path(filePath))) {
                        byte[] byte_read = new byte[readLength];
                        in.readFully(readPos, byte_read);
                        return byte_read;
                    } catch (IOException ex) {
                        LOG.warn("{}could not read file:{} ex:{}", new Object[]{logPrefix, filePath, ex.getMessage()});
                        return null;
                    } catch (Exception ex) {
                        LOG.error("{}unexpected exception:{}", logPrefix, ex);
                        throw new RuntimeException(ex);
                    }
                }
            });
            LOG.debug("{}reading done from file:{} from:{}", new Object[]{logPrefix, filePath, hopsURL});
            return result;
        } catch (Exception ex) {
            LOG.error("{}unexpected exception:{}", logPrefix, ex);
            throw new RuntimeException(ex);
        }
    }

    public static int append(HDFSResource resource, String user, final byte[] data) {
        final Configuration conf = new Configuration();
        String hopsURL = "hdfs://" + resource.hopsIp + ":" + resource.hopsPort;
        conf.set("fs.defaultFS", hopsURL);
        final String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        LOG.debug("{}appending to file:{} from:{}", new Object[]{logPrefix, filePath, hopsURL});
        UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
        try {
            int result = ugi.doAs(new PrivilegedExceptionAction<Integer>() {
                @Override
                public Integer run() throws Exception {
                    try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf);
                            FSDataOutputStream out = fs.append(new Path(filePath))) {
                        out.write(data);
                        out.flush();
                        return data.length;
                    } catch (IOException ex) {
                        LOG.warn("{}could not append to file:{} ex:{}", new Object[]{logPrefix, filePath, ex.getMessage()});
                        return -1;
                    } catch (Exception ex) {
                        LOG.error("{}unexpected exception:{}", logPrefix, ex);
                        throw new RuntimeException(ex);
                    }
                }
            });
            LOG.debug("{}appending done to file:{} from:{}", new Object[]{logPrefix, filePath, hopsURL});
            return result;
        } catch (Exception ex) {
            LOG.error("{}unexpected exception:{}", logPrefix, ex);
            throw new RuntimeException(ex);
        }
    }
}
