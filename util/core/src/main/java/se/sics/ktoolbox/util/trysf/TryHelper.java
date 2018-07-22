/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package se.sics.ktoolbox.util.trysf;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.javatuples.Pair;
import org.javatuples.Triplet;

public class TryHelper {

  public static Try<Boolean> tryStart() {
    return new Try.Success(true);
  }

  public static <I extends Object, O extends Object> BiFunction<I, Throwable, O> tryFSucc(Function<I, O> f) {
    return (I input, Throwable fail) -> {
      return f.apply(input);
    };
  }

  public static <I extends Object, O extends Object> BiFunction<I, Throwable, O> tryFFail(Function<Throwable, O> f) {
    return (I input, Throwable fail) -> {
      return f.apply(fail);
    };
  }

  public static Try<Pair> tryPair(Try input1, Try input2) {
    if (input1.isFailure()) {
      return (Try.Failure) input1;
    }
    if (input2.isFailure()) {
      return (Try.Failure) input2;
    }
    Pair tupleInput = Pair.with(input1.get(), input2.get());
    return new Try.Success(tupleInput);
  }

  public static <O extends Object> Try<Triplet> tryTriplet(Try<Pair<O, O>> input12, Try input3) {
    if (input12.isFailure()) {
      return (Try.Failure) input12;
    }
    if (input3.isFailure()) {
      return (Try.Failure) input3;
    }
    Triplet tupleInput = input12.get().addAt2(input3);
    return new Try.Success(tupleInput);
  }

  public static Throwable tryError(Try input) {
    if (input.isFailure()) {
      Try.Failure fail = (Try.Failure) input;
      try {
        fail.checkedGet();
        return new IllegalStateException("this should never happen");
      } catch (Throwable ex) {
        return ex;
      }
    } else {
      return new IllegalStateException("try was success - cannot extract exception");
    }
  }

  public static <I extends Object, O extends Object> Function<Try<I>, Try<O>> tryF(BiFunction<I, Throwable, Try<O>> f) {
    return (Try<I> t) -> {
      return t.flatMap(f);
    };
  }
}
