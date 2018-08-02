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
package se.sics.ktoolbox.httpsclient.builder;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * from
 * https://github.com/dropwizard/dropwizard
 */
public class JacksonMessageBodyProvider extends JacksonJaxbJsonProvider {

  private final ObjectMapper mapper;

  public JacksonMessageBodyProvider(ObjectMapper mapper) {
    this.mapper = mapper;
    setMapper(mapper);
  }

  @Override
  public boolean isReadable(Class<?> type,
    @Nullable Type genericType,
    @Nullable Annotation[] annotations,
    @Nullable MediaType mediaType) {
    return isProvidable(type) && super.isReadable(type, genericType, annotations, mediaType);
  }

  @Override
  public boolean isWriteable(Class<?> type,
    @Nullable Type genericType,
    @Nullable Annotation[] annotations,
    @Nullable MediaType mediaType) {
    return isProvidable(type) && super.isWriteable(type, genericType, annotations, mediaType);
  }

  private boolean isProvidable(Class<?> type) {
    final JsonIgnoreType ignore = type.getAnnotation(JsonIgnoreType.class);
    return (ignore == null) || !ignore.value();
  }

  public ObjectMapper getObjectMapper() {
    return mapper;
  }
}
