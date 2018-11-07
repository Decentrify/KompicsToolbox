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
package se.sics.ktoolbox.util.identifiable;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.SettableMemo;
import se.sics.ktoolbox.util.identifiable.basic.IntId;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.identifiable.basic.StringByteId;
import se.sics.ktoolbox.util.identifiable.basic.StringByteIdFactory;
import se.sics.ktoolbox.util.identifiable.basic.UUIDId;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IdentifierRegistryV2 {

  public static final Logger logger = LoggerFactory.getLogger(IdentifierRegistryV2.class);
  private static SettableMemo<ImmutableMap<String, Pair<Class, Function<Optional<Long>, IdentifierFactory>>>> identifierFactories
    = new SettableMemo("idRegistryV2");

  public final static Function<Optional<Long>, IdentifierFactory> UUID_FGEN = (Optional<Long> seed) -> {
    logger.warn("UUIDFactory does not have a settable seed");
    return new UUIDIdFactory();
  };

  public final static Function<Optional<Long>, IdentifierFactory> INTID_FGEN = (Optional<Long> seed) ->
    new IntIdFactory(seed);

  public final static Function<Integer, Function<Optional<Long>, IdentifierFactory>> STRINGBYTEID_FGEN
    = (Integer sequenceSize) -> (Optional<Long> seed) -> new StringByteIdFactory(seed, sequenceSize);

  public static void registerBaseDefaults1(int stringSequenceSize) {
    ImmutableMap.Builder<String, Pair<Class, Function<Optional<Long>, IdentifierFactory>>> factoryGenerators
      = ImmutableMap.builder();
    factoryGenerators.put(BasicIdentifiers.Values.EVENT.toString(), Pair.with(UUIDId.class, UUID_FGEN));
    factoryGenerators.put(BasicIdentifiers.Values.MSG.toString(), Pair.with(UUIDId.class, UUID_FGEN));
    factoryGenerators.put(BasicIdentifiers.Values.OVERLAY.toString(),
      Pair.with(StringByteId.class, STRINGBYTEID_FGEN.apply(stringSequenceSize)));
    factoryGenerators.put(BasicIdentifiers.Values.NODE.toString(), Pair.with(IntId.class, INTID_FGEN));
    identifierFactories.set(factoryGenerators.build());
  }

  public static IdentifierFactory instance(BasicIdentifiers.Values idType, Optional<Long> seed) {
    return identifierFactories.get().get(idType.toString()).getValue1().apply(seed);
  }

  public static Class idType(BasicIdentifiers.Values idName) {
    return identifierFactories.get().get(idName.toString()).getValue0();
  }
}
