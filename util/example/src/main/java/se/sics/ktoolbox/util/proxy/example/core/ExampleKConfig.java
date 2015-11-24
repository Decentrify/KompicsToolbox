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
package se.sics.ktoolbox.util.proxy.example.core;

import java.util.HashSet;
import java.util.Set;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption.Basic;
import se.sics.p2ptoolbox.util.config.options.BasicAddressOption;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExampleKConfig implements KConfigLevel {

    public final static Basic<Boolean> hookFail = new Basic("example.hookFail", Boolean.class, new ExampleKConfig());
    public final static Basic<Boolean> sendHPMsg = new Basic("example.sendMsg", Boolean.class, new ExampleKConfig());
    public final static BasicAddressOption localAdr = new BasicAddressOption("example.localAdr", new ExampleKConfig());
    public final static BasicAddressOption partnerAdr = new BasicAddressOption("example.partnerAdr", new ExampleKConfig());

    @Override
    public Set<String> canWrite() {
        Set<String> canWrite = new HashSet<>();
        canWrite.add(toString());
        return canWrite;
    }

    @Override
    public String toString() {
        return "ExampleKConfig";
    }
}
