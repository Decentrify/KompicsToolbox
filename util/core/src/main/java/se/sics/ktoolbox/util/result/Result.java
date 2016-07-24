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
        SUCCESS, BAD_REQUEST, TIMEOUT, INT_FAILURE, SAFE_EXT_FAILURE, UNSAFE_EXT_FAILURE;
    }

    public final Status status;
    /**
     * SUCCESS leads to the existance of an actual value of type V in value.
     * Failure of any kind leads to the existance of an Exception in the
     * value fields
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
    
    public static Result badRequest(Exception ex) {
        return failure(Result.Status.BAD_REQUEST, ex);
    }
    
    public static Result internalFailure(Exception ex) {
        return failure(Result.Status.INT_FAILURE, ex);
    }
    
    public static  Result externalSafeFailure(Exception ex) {
        return failure(Result.Status.SAFE_EXT_FAILURE, ex);
    }
    
    public static Result externalUnsafeFailure(Exception ex) {
        return failure(Result.Status.UNSAFE_EXT_FAILURE, ex);
    }
}