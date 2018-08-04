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

package se.sics.ktoolbox.webclient.builder;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * simplified
 * https://github.com/dropwizard/dropwizard
 */
public class JacksonFeature implements Feature {
    private final ObjectMapper mapper;

    public JacksonFeature(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new JacksonMessageBodyProvider(mapper), MessageBodyReader.class, MessageBodyWriter.class);
        return true;
    }
}