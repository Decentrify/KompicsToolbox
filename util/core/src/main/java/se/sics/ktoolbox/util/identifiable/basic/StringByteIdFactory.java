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
package se.sics.ktoolbox.util.identifiable.basic;

import com.google.common.io.BaseEncoding;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.Random;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.IdentifierBuilder;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class StringByteIdFactory implements IdentifierFactory<StringByteId> {

  private final Optional<Random> rand;
  private final int sequenceSize;
  private String registeredName;

  public StringByteIdFactory(Optional<Long> seed, int sequenceSize) {
    assert sequenceSize % 2 == 0;
    this.rand = seed.isPresent() ? Optional.of(new Random(seed.get())) : Optional.empty();
    this.sequenceSize = sequenceSize;
  }

  @Override
  public synchronized StringByteId randomId() {
    if(!rand.isPresent()) {
      throw new IllegalStateException("no seed");
    }
    byte[] bytes = new byte[sequenceSize / 2];
    rand.get().nextBytes(bytes);
    return id(new BasicBuilders.StringBuilder(BaseEncoding.base16().encode(bytes)));
  }

  @Override
  public StringByteId id(IdentifierBuilder builder) {
    byte[] base;
    String sBase;
    try {
      if (builder instanceof BasicBuilders.StringBuilder) {
        BasicBuilders.StringBuilder aux = (BasicBuilders.StringBuilder) builder;
        sBase = aux.base;
      } else if (builder instanceof BasicBuilders.ByteBuilder) {
        BasicBuilders.ByteBuilder aux = (BasicBuilders.ByteBuilder) builder;
        sBase = new String(aux.base, "UTF-8");
      } else if (builder instanceof BasicBuilders.IntBuilder) {
        BasicBuilders.IntBuilder aux = (BasicBuilders.IntBuilder) builder;
        sBase = Integer.toString(aux.base);
      } else if (builder instanceof BasicBuilders.UUIDBuilder) {
        BasicBuilders.UUIDBuilder aux = (BasicBuilders.UUIDBuilder) builder;
        sBase = aux.base.toString();
      } else {
        throw new UnsupportedOperationException("StringByteFactory does not support builder:" + builder.getClass());
      }
      base = sBase.getBytes("UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
    return new StringByteId(base, sBase);
  }

  @Override
  public Class<StringByteId> idType() {
    return StringByteId.class;
  }

  @Override
  public void setRegisteredName(String name) {
    this.registeredName = name;
  }

  @Override
  public String getRegisteredName() {
    return registeredName;
  }
}
