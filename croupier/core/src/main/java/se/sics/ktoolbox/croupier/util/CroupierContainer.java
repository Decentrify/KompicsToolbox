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
package se.sics.ktoolbox.croupier.util;

import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.other.AgingAdrContainer;
import se.sics.ktoolbox.util.update.View;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierContainer implements AgingAdrContainer<NatAwareAddress, View> {
    
    public final NatAwareAddress src;
    public final View content;
    int age;

    public CroupierContainer(NatAwareAddress src, View content, int age) {
        this.src = src;
        this.content = content;
        this.age = age;
    }

    public CroupierContainer(NatAwareAddress src, View content) {
        this(src, content, 0);
    }

    @Override
    public NatAwareAddress getSource() {
        return src;
    }
    
    @Override
    public View getContent() {
        return content;
    }
    
    @Override
    public int getAge() {
        return age;
    }

    @Override
    public void incrementAge() {
        age++;
    }
    
    public void resetAge() {
        age = 0;
    }

    /**
     * shallow copy - only age is not shared
     */
    public CroupierContainer copy() {
        return new CroupierContainer(src, content, age);
    }
    
    @Override
    public String toString() {
        return "<" + src.getId() + ":" + age + ">";
    }
}
