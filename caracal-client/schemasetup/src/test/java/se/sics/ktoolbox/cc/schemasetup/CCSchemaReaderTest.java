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

package se.sics.ktoolbox.cc.schemasetup;

import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCSchemaReaderTest {
    @Test
    public void test1() {
        Map<String, Map<String, String>> originalSchemas = new HashMap<String, Map<String, String>>();
        Map<String, String> schema1 = new HashMap<String, String>();
        schema1.put("algo", "paxos");
        schema1.put("db", "memory");
        schema1.put("vnodes", "1");
        schema1.put("rfactor", "3");
        Map<String, String> schema2 = new HashMap<String, String>();
        schema2.put("vnodes", "5");
        originalSchemas.put("gvod.heartbeat", schema1);
        originalSchemas.put("gvod.metadata", schema2);
        
        Map<String, Map<String, String>> fileSchemas = CCSchemaReader.readSchemas("schemaFile1");
        System.out.println(fileSchemas);
        Assert.assertEquals(originalSchemas.size(), fileSchemas.size());
        for(Map.Entry<String, Map<String, String>> se : originalSchemas.entrySet()) {
            Assert.assertTrue(fileSchemas.containsKey(se.getKey()));
            for(Map.Entry<String, String> e : se.getValue().entrySet()) {
                Map<String, String> schema = fileSchemas.get(se.getKey());
                Assert.assertTrue(schema.containsKey(e.getKey()));
                Assert.assertEquals(e.getValue(), schema.get(e.getKey()));
            }
        }
    }
}
