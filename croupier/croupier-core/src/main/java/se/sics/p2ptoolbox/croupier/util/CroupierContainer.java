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
package se.sics.p2ptoolbox.croupier.util;

import se.sics.p2ptoolbox.util.Container;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.traits.Ageing;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierContainer<CV extends Object> implements Container<DecoratedAddress, CV>, Ageing {

    private int age;
    private DecoratedAddress src;
    private final CV content;

    public CroupierContainer(DecoratedAddress src, CV content, int age) {
        this.age = age;
        this.src = src;
        this.content = content;
    }

    public CroupierContainer(DecoratedAddress src, CV content) {
        this(src, content, 0);
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public DecoratedAddress getSource() {
        return src;
    }

    @Override
    public CV getContent() {
        return content;
    }

    public void incrementAge() {
        age++;
    }

    /**
     * shallow copy - only age is not shared
     */
    public CroupierContainer<CV> getCopy() {
        return new CroupierContainer(src, content, age);
    }
    
    @Override
    public String toString() {
        return "<" + src + ":" + age + ">";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + this.age;
        hash = 53 * hash + (this.src != null ? this.src.hashCode() : 0);
        hash = 53 * hash + (this.content != null ? this.content.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CroupierContainer<?> other = (CroupierContainer<?>) obj;
        if (this.age != other.age) {
            return false;
        }
        if (this.src != other.src && (this.src == null || !this.src.equals(other.src))) {
            return false;
        }
        if (this.content != other.content && (this.content == null || !this.content.equals(other.content))) {
            return false;
        }
        return true;
    }
}
