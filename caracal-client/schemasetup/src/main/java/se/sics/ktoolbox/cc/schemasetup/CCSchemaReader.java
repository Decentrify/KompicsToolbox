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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCSchemaReader {

    private final static Logger LOG = LoggerFactory.getLogger(CCSchemaSetupComp.class);
    static final Charset CHARSET = Charset.forName("UTF8");
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Map<String, Map<String, String>> readSchemas(String schemaFile) {
        List<String> jsons = new LinkedList<String>();
        try {
            // try as resource
            URL inUrl = ClassLoader.getSystemResource(schemaFile);
            InputStream in;
            if (inUrl != null) {
                in = inUrl.openStream();
            } else {
                LOG.warn("Couldn't find resource '{}'!", schemaFile);
                // try as file
                try {
                    in = new FileInputStream(schemaFile);
                } catch (FileNotFoundException ex) {
                    LOG.warn("Can't find given file: {}", ex);
                    throw new RuntimeException(ex);
                } catch (SecurityException ex) {
                    LOG.warn("Not allowed to access file: {}", ex);
                    throw new RuntimeException(ex);
                }
            }

            InputStreamReader reader = new InputStreamReader(in, CHARSET);
            BufferedReader breader = new BufferedReader(reader);
            try {
                LOG.info("Importing Schema: {}", schemaFile);
                StringBuilder sb = new StringBuilder();
                while (breader.ready()) {
                    sb.append(breader.readLine());
                }
                jsons.add(sb.toString());
            } finally {
                reader.close();
                breader.close();
                in.close();
            }

        } catch (IOException ex) {
            LOG.error("While importing schemas: \n {}", ex);
        }

        return readSchemas(jsons);
    }

    public static Map<String, Map<String, String>> readSchemas(Collection<String> json) {
        Map<String, Map<String, String>> schemaMap = new HashMap<String, Map<String, String>>();

        Type collectionType = new TypeToken<LinkedList<SchemaObj>>() {
        }.getType();
        for (String jsonS : json) {
            LinkedList<SchemaObj> schemaList = GSON.fromJson(jsonS, collectionType);
            for (SchemaObj schemaObj : schemaList) {
                schemaMap.put(schemaObj.name, schemaObj.meta);
            }
        }
        return schemaMap;
    }

    static class SchemaObj {

        String name;
        HashMap<String, String> meta = new HashMap<String, String>();

        public SchemaObj() {
        }

        @Override
        public String toString() {
            return name + ": " + meta;
        }
    }
}
