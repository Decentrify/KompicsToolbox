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
package se.sics.ktoolbox.util.managedStore.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FileResourceRegistry {
    private static final Map<String, FileResourceMngr> frMngrMap = new ConcurrentHashMap<>();
    
    public synchronized static void register(String fileResourceType, FileResourceMngr frMngr) {
        if(frMngrMap.containsKey(fileResourceType)) {
            throw new RuntimeException("file resource type:" + fileResourceType + " already registered");
        }
        frMngrMap.put(fileResourceType, frMngr);
    }
    
    public static FileResourceMngr getMngr(String fileResourceType) {
         if(!frMngrMap.containsKey(fileResourceType)) {
            throw new RuntimeException("file resource type:" + fileResourceType + " is not registered");
        }
         return frMngrMap.get(fileResourceType);
    }
}
