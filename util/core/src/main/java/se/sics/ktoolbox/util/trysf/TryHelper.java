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

import io.netty.buffer.ByteBufUtil;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.javatuples.Pair;
import org.javatuples.Triplet;

public class TryHelper {

  public static Try<Boolean> tryStart() {
    return new Try.Success(true);
  }

  public static <I, O> BiFunction<I, Throwable, O> tryFSucc0(Supplier<O> s) {
    return (I input, Throwable fail) -> {
      return s.get();
    };
  }

  public static <I, O> BiFunction<I, Throwable, O> tryFSucc1(Function<I, O> f) {
    return (I input, Throwable fail) -> {
      return f.apply(input);
    };
  }

  public static <I1, I2, O> BiFunction<Pair<I1, I2>, Throwable, O>
    tryFSucc2(Function<I1, Function<I2, O>> f) {
    return (Pair<I1, I2> input, Throwable fail) -> {
      return f.apply(input.getValue0()).apply(input.getValue1());
    };
  }

  public static <I1, I2, I3, O> BiFunction<Triplet<I1, I2, I3>, Throwable, O>
    tryFSucc3(Function<I1, Function<I2, Function<I3, O>>> f) {
    return (Triplet<I1, I2, I3> input, Throwable fail) -> {
      Function<I2, Function<I3, O>> pf1 = f.apply(input.getValue0());
      Function<I3, O> pf2 = pf1.apply(input.getValue1());
      O result = pf2.apply(input.getValue2());
      return result;
    };
  }

  public static <I, T extends Throwable, O> BiFunction<I, T, O> tryFFail(Function<T, O> f) {
    return (I input, T fail) -> {
      return f.apply(fail);
    };
  }

  public static class Joiner {

    public static <I1, I2> Try<I2> map(Try<I1> input1, Try<I2> input2) {
      if (input1.isFailure()) {
        return (Try.Failure) input1;
      }
      return input2;
    }

    public static <I1, I2> Try<Pair<I1, I2>> combine(I1 in1, Try<I2> in2) {
      if (in2.isFailure()) {
        return (Try.Failure) in2;
      }
      Pair<I1, I2> tupleInput = Pair.with(in1, in2.get());
      return new Try.Success(tupleInput);
    }

    public static <I1, I2> Try<Pair<I1, I2>> combine(Try<I1> in1, Try<I2> in2) {
      if (in1.isFailure()) {
        return (Try.Failure) in1;
      }
      if (in2.isFailure()) {
        return (Try.Failure) in2;
      }
      Pair<I1, I2> tupleInput = Pair.with(in1.get(), in2.get());
      return new Try.Success(tupleInput);
    }

    public static <I1, I2, I3> Try<Triplet<I1, I2, I3>> combine(Try<I1> in1, Try<I2> in2, Try<I3> in3) {
      if (in1.isFailure()) {
        return (Try.Failure) in1;
      }
      if (in2.isFailure()) {
        return (Try.Failure) in2;
      }
      if (in3.isFailure()) {
        return (Try.Failure) in3;
      }
      Triplet<I1, I2, I3> tupleInput = Triplet.with(in1.get(), in2.get(), in3.get());
      return new Try.Success(tupleInput);
    }

    public static <I1, I2, I3> Try<Triplet<I1, I2, I3>> combine2(Try<Pair<I1, I2>> input12, Try<I3> input3) {
      if (input12.isFailure()) {
        return (Try.Failure) input12;
      }
      if (input3.isFailure()) {
        return (Try.Failure) input3;
      }
      Triplet<I1, I2, I3> tupleInput = input12.get().addAt2(input3.get());
      return new Try.Success(tupleInput);
    }

    public static <I> Try<String> successMsg(Try<I> in, String msg) {
      if (in.isSuccess()) {
        return new Try.Success(String.format(msg, in.get().toString()));
      }
      return (Try.Failure) in;
    }
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

  public static BiFunction<Pair<byte[], byte[]>, Throwable, Boolean> compareArray() {
    return tryFSucc2((byte[] array1) -> (byte[] array2) -> {
      boolean result = Arrays.equals(array1, array2);
      return result;
    });
  }

  public static class SimpleCollector<R> {

    private Try<Boolean> joinedResult = new Try.Success(true);
    private int pendingResults;

    public SimpleCollector(int size) {
      pendingResults = size;
    }

    public void collect(Try<R> result) {
      pendingResults--;
      joinedResult = TryHelper.Joiner.map(result, joinedResult);
    }

    public boolean completed() {
      return pendingResults == 0;
    }

    public Try<Boolean> getResult() {
      return joinedResult;
    }
  }
}
