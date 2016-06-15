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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.slf4j.LoggerFactory;

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
            LOG.debug("{}getting status to:{}", logPrefix, hopsURL);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean delete(String hopsIp, int hopsPort, String dirPath, String fileName) {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsIp + ":" + hopsPort);
        try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf)) {
            fs.delete(new Path(dirPath + Path.SEPARATOR + fileName), false);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean create(String hopsIp, int hopsPort, String dirPath, String fileName, long fileSize) {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsIp + ":" + hopsPort);
        try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf)) {
            if (!fs.isDirectory(new Path(dirPath))) {
                fs.mkdirs(new Path(dirPath));
            }
            String filePath = dirPath + Path.SEPARATOR + fileName;
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
                if(fileSize % 1024 != 0) {
                    byte[] data = new byte[(int)(fileSize % 1024)];
                    rand.nextBytes(data);
                    out.write(data);
                    out.flush();
                }
                return true;
            } catch (IOException ex) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
    }
}
