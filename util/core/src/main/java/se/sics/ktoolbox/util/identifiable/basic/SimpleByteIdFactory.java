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

import com.google.common.primitives.Ints;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.IdentifierBuilder;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SimpleByteIdFactory implements IdentifierFactory<SimpleByteId> {

  private final Optional<Random> rand;
  private final int randomSize;
  private String registeredName;

  public SimpleByteIdFactory(Optional<Long> seed, int sequenceSize) {
    this.rand = seed.isPresent() ? Optional.of(new Random(seed.get())) : Optional.empty();
    this.randomSize = sequenceSize;
  }

  @Override
  public synchronized SimpleByteId randomId() {
    if(!rand.isPresent()) {
      throw new IllegalStateException("no seed");
    }
    byte[] bytes = new byte[randomSize];
    rand.get().nextBytes(bytes);
    return new SimpleByteId(bytes);
  }

  @Override
  public SimpleByteId id(IdentifierBuilder builder) {
    byte[] base;
    if (builder instanceof BasicBuilders.ByteBuilder) {
      base = ((BasicBuilders.ByteBuilder) builder).base;
    } else if (builder instanceof BasicBuilders.StringBuilder) {
      BasicBuilders.StringBuilder aux = (BasicBuilders.StringBuilder) builder;
      try {
        base = aux.base.getBytes("UTF-8");
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    } else if (builder instanceof BasicBuilders.IntBuilder) {
      BasicBuilders.IntBuilder aux = (BasicBuilders.IntBuilder) builder;
      base = Ints.toByteArray(aux.base);
    } else if (builder instanceof BasicBuilders.UUIDBuilder) {
      BasicBuilders.UUIDBuilder aux = (BasicBuilders.UUIDBuilder) builder;
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(aux.base.getMostSignificantBits());
      bb.putLong(aux.base.getLeastSignificantBits());
      base = bb.array();
    } else {
      throw new UnsupportedOperationException("SimpleByteIdFactory does not support builder:" + builder.getClass());
    }
    return new SimpleByteId(base);
  }

  @Override
  public Class<SimpleByteId> idType() {
    return SimpleByteId.class;
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
