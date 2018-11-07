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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SettableMemo<O> {
  private final String name;
  private final AtomicBoolean setFlag = new AtomicBoolean(false);
  private O value;
  
  public SettableMemo(String name) {
    this.name = name;
  }
  
  public boolean isSet() {
    return setFlag.get();
  }
  
  public void set(O value) {
    this.value = value;
    if(!setFlag.compareAndSet(false, true)) {
      throw new IllegalStateException("setting " + name + " a second time");
    }
  }
  
  public O get() {
    if(!isSet()) {
      throw new IllegalStateException(name + "is not set");
    }
    return value;
  }
}
