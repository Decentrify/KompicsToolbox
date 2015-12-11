/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.gradient.util;

import se.sics.kompics.network.Address;
import se.sics.ktoolbox.util.Wrapper;
import se.sics.ktoolbox.util.other.Container;
import se.sics.ktoolbox.util.traits.Ageing;


/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientContainer<C extends Object> implements Container<Address, C>, Ageing, Wrapper<C> {

    private int age;
    private Address src;
    public final int rank;
    private final C content;

    public GradientContainer(Address src, C content, int age, int rank) {
        this.age = age;
        this.src = src;
        this.rank = rank;
        this.content = content;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public Address getSource() {
        return src;
    }

    @Override
    public C getContent() {
        return content;
    }

    public void incrementAge() {
        age++;
    }

    /**
     * shallow copy - only age is not shared
     */
    public GradientContainer<C> getCopy() {
        return new GradientContainer(src, content, age, rank);
    }
    
    @Override
    public String toString() {
        return "<" + src + "," + age + ">:rank:" + rank + ":value:" + content;
    }

    //*********************ComparableWrapper************************************
    @Override
    public C unwrap() {
        return content;
    }

    @Override
    public Container copy() {
        return new GradientContainer(src, content, age, rank);
    }
}
