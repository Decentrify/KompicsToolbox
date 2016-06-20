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
package basic;

import java.io.IOException;
import org.junit.Test;
import se.sics.ktoolbox.hops.managedStore.storage.HDFSHelper;
import se.sics.ktoolbox.hops.managedStore.storage.util.HDFSResource;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestHDFSHelper {
    @Test
    public void simpleTest() throws IOException, InterruptedException {
        HDFSResource resource = new HDFSResource("bbc1.sics.se", 26801, "/experiment/upload", "file");
        for(int i = 0; i < 1000; i++) {
            System.out.println("read:" + i);
            HDFSHelper.read(resource, "glassfish", i*1024, (i+1)*1024);
        }
    }
}
