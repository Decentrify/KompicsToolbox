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
package se.sics.ktoolbox.hdfs;

import java.io.File;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSResource {

    public final Configuration hdfsConfig;
    public final String user;
    public final String dirPath;
    public final String fileName;

    public HDFSResource(String hopsIp, int hopsPort, String user, String dirPath, String fileName) {
        this.hdfsConfig = new Configuration();
        String hopsURL = "hdfs://" + hopsIp + ":" + hopsPort;
        hdfsConfig.set("fs.defaultFS", hopsURL);
        this.user = user;
        this.dirPath = dirPath;
        this.fileName = fileName;
    }

    public HDFSResource(String hdfsXMLPath, String user, String dirPath, String fileName) {
        this.hdfsConfig = new Configuration();
        //TODO Alex - I know, shouldn't throw exceptions in constructors
        //maybe later I will make it as a static method to do the checks before invoking constructor
        File confFile = new File(hdfsXMLPath);
        if(!confFile.exists()) {
            throw new RuntimeException("conf file does not exist");
        }
        this.hdfsConfig.addResource(new Path(hdfsXMLPath));
        this.user = user;
        this.dirPath = dirPath;
        this.fileName = fileName;
    }
}
