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

import java.util.Arrays;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 *
 * @author jsvhqr
 */
public class testStorage {
    
    
    public static void main(String [] args){
        
        
        String endpoint = "hdfs://bbc1.sics.se:8020";
        String path = "/tester1234.test";
        String example = "This is my example";
        
        Storage s = new HopsDataStorage(path, endpoint,null,null,null);
        
        byte [] bytes = example.getBytes();
        
        s.write(0, bytes);
        
        System.out.println("Wrote.." + bytes.length + " to file " + path);
        
        byte [] read = s.read(0, bytes.length);
        
        for(int i = 0; i< read.length; i++){
            System.out.print(read[i]);
            System.out.println();
        }
        
        
        
    }
    
}
