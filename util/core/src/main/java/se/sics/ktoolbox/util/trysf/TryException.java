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
package se.sics.ktoolbox.util.trysf;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TryException {

  public static class Chain extends Exception {

    public final Throwable cause;
    public final List<Throwable> andThen = new LinkedList<>();

    public Chain(String msg) {
      this(new Base(msg));
    }

    public Chain(String msg, Throwable cause) {
      this(new Base(msg, cause));
    }

    public Chain(Base cause) {
      this.cause = cause;
    }

    public Chain andThen(Throwable cause) {
      andThen.add(cause);
      return this;
    }
  }

  public static class Base extends Exception {

    public Base(String msg) {
      super(msg);
    }

    public Base(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
