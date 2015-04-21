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

package se.sics.p2ptoolbox.caracalclient.schemasetup;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCSchemaSetup {
    private final static Logger log = LoggerFactory.getLogger(CCSchemaSetupComp.class);

    public final Map<String, Map<String, String>> schemas;
    
    public CCSchemaSetup(Config config) {
        schemas = new HashMap<String, Map<String, String>>();
        try {
            List<String> schemaFiles = config.getStringList("caracal-client.schema.names");
            for(String schemaFile : schemaFiles) {
                schemas.putAll(CCSchemaReader.readSchemas(schemaFile));
            }
        } catch(ConfigException.Missing ex) {
            log.error("bad configuration - missing parameter:{}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            log.error("configuration exception:{}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
