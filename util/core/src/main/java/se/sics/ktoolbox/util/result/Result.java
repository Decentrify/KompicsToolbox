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
     * Failure of any kind leads to the existance of a description String in the
     * value fields
     */
    private final Either<V, String> value;
    
    public Result(Status status, Either<V, String> value) {
        this.status = status;
        this.value = value;
    }
    
    public boolean isSuccess() {
        return status.equals(Status.SUCCESS);
    }
    
    public V getValue() {
        return value.getLeft();
    }
    
    public String getErrorDescription() {
        return value.getRight();
    }

    public static <V extends Object> Result<V> success(V value) {
        Either<V, String> evalue = Either.left(value);
        return new Result(Status.SUCCESS, evalue);
    }
    
    public static <V extends Object> Result<V> failure(Status status, String description) {
        Either<V, String> evalue = Either.right(description);
        return new Result(status, evalue);
    }
    
    public static <V extends Object> Result<V> timeout(String description) {
        return failure(Result.Status.TIMEOUT, description);
    }
    
    public static <V extends Object> Result<V> badRequest(String description) {
        return failure(Result.Status.BAD_REQUEST, description);
    }
    
    public static <V extends Object> Result<V> internalFailure(String description) {
        return failure(Result.Status.INT_FAILURE, description);
    }
    
    public static <V extends Object> Result<V> externalSafeFailure(String description) {
        return failure(Result.Status.SAFE_EXT_FAILURE, description);
    }
    
    public static <V extends Object> Result<V> externalUnsafeFailure(String description) {
        return failure(Result.Status.UNSAFE_EXT_FAILURE, description);
    }
}