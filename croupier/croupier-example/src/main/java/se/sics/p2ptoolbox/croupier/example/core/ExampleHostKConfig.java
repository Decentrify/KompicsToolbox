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
package se.sics.p2ptoolbox.croupier.example.core;

import java.util.HashSet;
import java.util.Set;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption;
import se.sics.p2ptoolbox.util.config.options.OpenAddressBootstrapOption;
import se.sics.p2ptoolbox.util.config.options.OpenAddressOption;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExampleHostKConfig implements KConfigLevel {

    public final static OpenAddressOption self = new OpenAddressOption("self", new ExampleHostKConfig());
    public final static KConfigOption.Basic<Long> seed = new KConfigOption.Basic("seed", Long.class, new ExampleHostKConfig());
    public final static KConfigOption.Basic<Boolean> observer = new KConfigOption.Basic("observer", Boolean.class, new ExampleHostKConfig());
    public final static OpenAddressBootstrapOption bootstrap = new OpenAddressBootstrapOption("bootstrap", new ExampleHostKConfig());
    
    @Override
    public Set<String> canWrite() {
        Set<String> canWrite = new HashSet<>();
        canWrite.add(toString());
        return canWrite;
    }

    @Override
    public String toString() {
        return "NetworkMngrConfig";
    }
}
