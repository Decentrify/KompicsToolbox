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

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Try<T> {

  private Try() {
  }

  public abstract boolean isSuccess();

  public abstract boolean isFailure();

  public abstract T get() throws TryFailException;

  public abstract T checkedGet() throws Throwable;

  public abstract Try<Throwable> failed();

  public abstract Optional<T> toOptional();

  public abstract <U> Try<U> map(
    BiFunction<? super T, Throwable, ? extends U> mapper);

  public abstract <U> Try<U> flatMap(
    BiFunction<? super T, Throwable, ? extends Try<U>> mapper);

  public abstract <U> Try<U> transform(
    BiFunction<? super T, Throwable, ? extends Try<U>> successFunc,
    BiFunction<? super T, Throwable, ? extends Try<U>> failureFunc);

  public abstract <U> Try<U> recover(
    BiFunction<? super T, Throwable, ? extends U> recoverFunc);

  public abstract <U> Try<U> recoverWith(
    BiFunction<? super T, Throwable, ? extends Try<U>> recoverFunc);

  public abstract Try<T> orElse(Try<T> defaultValue);

  public abstract T getOrElse(T defaultValue);

  public static <T> Try<T> join(Try<Try<T>> t) {
    if (t == null) {
      return ((Try<T>) new Failure<>(new NullPointerException("t is null")));
    } else if (t instanceof Failure<?>) {
      return ((Try<T>) t);
    } else {
      return t.get();
    }
  }

  public static <T> Try<T> apply(FailableSupplier<T> supplier) {
    try {
      return new Success<>(supplier.get());
    } catch (Throwable e) {
      if (e instanceof Exception) {
        return new Failure<>((Exception) e);
      } else {
        throw ((Error) e);
      }
    }
  }

  public static <T extends AutoCloseable, R> Function<T, Try<R>> apply(
    Function<T, R> consumer) {
    return (closeable) -> Try.apply(() -> {
      try (T in = closeable) {
        return consumer.apply(in);
      }
    });
  }

  public static final class Success<T> extends Try<T> {

    private final T value;

    public Success(T value) {
      super();
      this.value = value;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public T checkedGet() {
      return get();
    }

    @Override
    public Try<Throwable> failed() {
      return new Failure<>(new UnsupportedOperationException("Success.failed"));
    }

    @Override
    public Optional<T> toOptional() {
      return Optional.ofNullable(value);
    }

    @Override
    public <U> Try<U> map(BiFunction<? super T, Throwable, ? extends U> mapper) {
      return Try.apply(() -> mapper.apply(value, null));
    }

    @Override
    public <U> Try<U> flatMap(BiFunction<? super T, Throwable, ? extends Try<U>> mapper) {
      return Try.join(Try.apply(() -> mapper.apply(value, null)));
    }

    @Override
    public <U> Try<U> transform(
      BiFunction<? super T, Throwable, ? extends Try<U>> successFunc,
      BiFunction<? super T, Throwable, ? extends Try<U>> failureFunc) {
      return Try.join(Try.apply(() -> successFunc.apply(value, null)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Try<U> recover(
      BiFunction<? super T, Throwable, ? extends U> recoverFunc) {
      return (Try<U>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Try<U> recoverWith(
      BiFunction<? super T, Throwable, ? extends Try<U>> recoverFunc) {
      return (Try<U>) this;
    }

    @Override
    public T getOrElse(T defaultValue) {
      return value;
    }

    @Override
    public Try<T> orElse(Try<T> defaultValue) {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Success success = (Success) o;

      return value.equals(success.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      return "Success{" + "value=" + value + '}';
    }
  }

  public static final class Failure<T> extends Try<T> {

    private final Throwable exception;
    private final TryFailException unckeckedException;

    public Failure(Throwable exception) {
      super();
      this.exception = exception;
      this.unckeckedException = new TryFailException(exception);
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public T get() {
      throw unckeckedException;
    }

    @Override
    public T checkedGet() throws Throwable {
      throw exception;
    }

    @Override
    public Try<Throwable> failed() {
      return new Success<>(exception);
    }

    @Override
    public Optional<T> toOptional() {
      return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Try<U> map(BiFunction<? super T, Throwable, ? extends U> mapper) {
      return (Try<U>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Try<U> flatMap(BiFunction<? super T, Throwable, ? extends Try<U>> mapper) {
      return (Try<U>) this;
    }

    @Override
    public <U> Try<U> transform(
      BiFunction<? super T, Throwable, ? extends Try<U>> successFunc,
      BiFunction<? super T, Throwable, ? extends Try<U>> failureFunc) {
      return Try.join(Try.apply(() -> failureFunc.apply(null, exception)));
    }

    @Override
    public <U> Try<U> recover(
      BiFunction<? super T, Throwable, ? extends U> recoverFunc) {
      return Try.apply(() -> recoverFunc.apply(null, exception));
    }

    @Override
    public <U> Try<U> recoverWith(
      BiFunction<? super T, Throwable, ? extends Try<U>> recoverFunc) {
      return Try.join(Try.apply(() -> recoverFunc.apply(null, exception)));
    }

    @Override
    public T getOrElse(T defaultValue) {
      return defaultValue;
    }

    @Override
    public Try<T> orElse(Try<T> defaultValue) {
      return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Failure failure = (Failure) o;

      return failure.exception.equals(exception);

    }

    @Override
    public int hashCode() {
      return exception.hashCode();
    }

    @Override
    public String toString() {
      return "Failure{" + "exception=" + exception + '}';
    }
  }
}
