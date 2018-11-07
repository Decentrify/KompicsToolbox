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
import java.util.Optional;
import java.util.Random;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.IdentifierBuilder;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IntIdFactory implements IdentifierFactory<IntId> {

  private final Optional<Random> rand;
  private String registeredName;

  public IntIdFactory(Optional<Long> seed) {
    this.rand = seed.isPresent() ? Optional.of(new Random(seed.get())) : Optional.empty();
  }

  @Override
  public synchronized IntId randomId() {
    if (!rand.isPresent()) {
      throw new IllegalStateException("no seed");
    }
    return new IntId(rand.get().nextInt());
  }

  @Override
  public IntId id(IdentifierBuilder builder) {
    int base;
    if (builder instanceof BasicBuilders.IntBuilder) {
      BasicBuilders.IntBuilder aux = (BasicBuilders.IntBuilder) builder;
      base = aux.base;
    } else if (builder instanceof BasicBuilders.StringBuilder) {
      BasicBuilders.StringBuilder aux = (BasicBuilders.StringBuilder) builder;
      base = Integer.valueOf(aux.base);
    } else if (builder instanceof BasicBuilders.ByteBuilder) {
      BasicBuilders.ByteBuilder aux = (BasicBuilders.ByteBuilder) builder;
      base = Ints.fromByteArray(aux.base);
    } else if (builder instanceof BasicBuilders.UUIDBuilder) {
      throw new UnsupportedOperationException("IntFactory does not support uuid builder");
    } else {
      throw new UnsupportedOperationException("IntFactory does not support builder" + builder.getClass());
    }
    return new IntId(base);
  }

  public IntId rawId(int val) {
    return new IntId(val);
  }

  @Override
  public Class<IntId> idType() {
    return IntId.class;
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
