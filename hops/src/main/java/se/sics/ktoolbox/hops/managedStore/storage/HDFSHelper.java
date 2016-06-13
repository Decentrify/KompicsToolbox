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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSHelper {
    public static boolean canConnect(String hopsIp, int hopsPort) {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsIp + ":" + hopsPort);
        try(DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf)) {
            FsStatus status = fs.getStatus();
            return true;
        } catch (IOException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static void delete(String hopsIp, int hopsPort, String dirPath, String fileName) {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsIp + ":" + hopsPort);
        try(DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf)) {
            fs.delete(new Path(dirPath + Path.SEPARATOR + fileName), false);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } 
    }
}
