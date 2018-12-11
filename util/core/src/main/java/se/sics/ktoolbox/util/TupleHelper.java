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
package se.sics.ktoolbox.util;

import java.util.function.Consumer;
import java.util.function.Function;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Triplet;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TupleHelper {
  public static <I1, I2> PairConsumer<I1, I2> pairConsumer(Function<I1, Consumer<I2>> f) {
    return new PairConsumer<I1,I2>() {
      @Override
      public void accept(I1 i1, I2 i2) {
        f.apply(i1).accept(i2);
      }
    };
  }
  public static <I1, I2, I3> Consumer<Triplet<I1, I2, I3>> consumer(Function<I1, Function<I2, Consumer<I3>>> f) {
    return (in) -> f.apply(in.getValue0()).apply(in.getValue1()).accept(in.getValue2());
  }
  
  public static <I1, I2, I3> TripletConsumer<I1, I2, I3> tripletConsumer(Function<I1, Function<I2, Consumer<I3>>> f) {
    return new TripletConsumer<I1,I2,I3>() {
      @Override
      public void accept(I1 i1, I2 i2, I3 i3) {
        f.apply(i1).apply(i2).accept(i3);
      }
    };
  }
  
  public static <I1, I2, I3, I4> QuartetConsumer<I1, I2, I3, I4> quartetConsumer(
    Function<I1, Function<I2, Function<I3, Consumer<I4>>>> f) {
    return new QuartetConsumer<I1,I2,I3,I4>() {
      @Override
      public void accept(I1 i1, I2 i2, I3 i3, I4 i4) {
        f.apply(i1).apply(i2).apply(i3).accept(i4);
      }
    };
  }
  
  public static abstract class PairConsumer<I1, I2> implements Consumer<Pair<I1,I2>> {

    @Override
    public void accept(Pair<I1,I2> in) {
      accept(in.getValue0(), in.getValue1());
    }
    
    public abstract void accept(I1 i1, I2 i2);
  }
  
  public static abstract class TripletConsumer<I1, I2, I3> implements Consumer<Triplet<I1,I2,I3>> {

    @Override
    public void accept(Triplet<I1,I2,I3> in) {
      accept(in.getValue0(), in.getValue1(), in.getValue2());
    }
    
    public abstract void accept(I1 i1, I2 i2, I3 i3);
  }
  
  public static abstract class QuartetConsumer<I1, I2, I3, I4> implements Consumer<Quartet<I1,I2,I3, I4>> {

    @Override
    public void accept(Quartet<I1,I2,I3, I4> in) {
      accept(in.getValue0(), in.getValue1(), in.getValue2(), in.getValue3());
    }
    
    public abstract void accept(I1 i1, I2 i2, I3 i3, I4 i4);
  }
}
