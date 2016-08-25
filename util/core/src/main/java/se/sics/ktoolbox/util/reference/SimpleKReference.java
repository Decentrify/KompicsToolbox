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
package se.sics.ktoolbox.util.reference;

import com.google.common.base.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
class SimpleKReference<V extends Object> implements KReference<V> {
    private final AtomicInteger counter;
    private Optional<V> value;
    
    public SimpleKReference(V value) {
        this.counter =  new AtomicInteger(1);
        this.value = Optional.of(value);
    }
   
    @Override
    public boolean isValid() {
        return counter.get() != 0;
    } 
    
    @Override
    public synchronized boolean retain() {
        if(!isValid()) {
            return false;
        } 
        counter.incrementAndGet();
        return true;
    }
    
    @Override
    public synchronized void release() throws KReferenceException {
        if(!isValid()) {
            throw new KReferenceException("relase of an invalid reference");
        }
        if(counter.decrementAndGet() == 0) {
            value = Optional.absent();
        }
    }
    
    @Override
    public Optional<V> getValue() {
        return value;
    }
}
