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
package se.sics.ktoolbox.util.result;

import se.sics.ktoolbox.util.Either;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Result<V extends Object> {

    /**
     * UNSAFE - might have side effects
     */
    public static enum Status {

        SUCCESS((byte) 0), BUSY((byte) 1), TIMEOUT((byte) 2), BAD_REQUEST((byte) 3), INT_FAILURE((byte) 4), SAFE_EXT_FAILURE((byte) 5), UNSAFE_EXT_FAILURE((byte) 6);

        public final byte code;

        Status(byte code) {
            this.code = code;
        }

        public static boolean isSuccess(Status status) {
            return SUCCESS.equals(status);
        }

        public static Status statusFrom(byte code) {
            switch (code) {
                case 0:
                    return SUCCESS;
                case 1:
                    return BUSY;
                case 2:
                    return TIMEOUT;
                case 3:
                    return BAD_REQUEST;
                case 4:
                    return INT_FAILURE;
                case 5:
                    return SAFE_EXT_FAILURE;
                case 6:
                    return UNSAFE_EXT_FAILURE;
                default:
                    throw new RuntimeException("unknown status code");
            }
        }
    }

    public final Status status;
    /**
     * SUCCESS leads to the existance of an actual value of type V in value.
     * Failure of any kind leads to the existance of an Exception in the value
     * fields
     */
    private final Either<V, Exception> value;

    public Result(Status status, Either<V, Exception> value) {
        this.status = status;
        this.value = value;
    }

    public boolean isSuccess() {
        return status.equals(Status.SUCCESS);
    }

    public V getValue() {
        return value.getLeft();
    }

    public Exception getException() {
        return value.getRight();
    }

    public String getExceptionDescription() {
        return value.getRight().getMessage();
    }

    public static <V extends Object> Result<V> success(V value) {
        Either<V, String> evalue = Either.left(value);
        return new Result(Status.SUCCESS, evalue);
    }

    public static Result failure(Status status, Exception ex) {
        Either evalue = Either.right(ex);
        return new Result(status, evalue);
    }

    public static Result timeout(Exception ex) {
        return failure(Result.Status.TIMEOUT, ex);
    }

    public static Result badArgument(String cause) {
        return failure(Result.Status.BAD_REQUEST, new IllegalArgumentException(cause));
    }
    public static Result badRequest(Exception ex) {
        return failure(Result.Status.BAD_REQUEST, ex);
    }

    public static Result internalStateFailure(String msg) {
        return failure(Result.Status.INT_FAILURE, new IllegalStateException(msg));
    }
    public static Result internalFailure(Exception ex) {
        return failure(Result.Status.INT_FAILURE, ex);
    }

    public static Result externalSafeFailure(Exception ex) {
        return failure(Result.Status.SAFE_EXT_FAILURE, ex);
    }

    public static Result externalUnsafeFailure(Exception ex) {
        return failure(Result.Status.UNSAFE_EXT_FAILURE, ex);
    }
}
