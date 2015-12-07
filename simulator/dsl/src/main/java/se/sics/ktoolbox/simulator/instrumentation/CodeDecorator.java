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
package se.sics.ktoolbox.simulator.instrumentation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import se.sics.kompics.JavaComponent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CodeDecorator {
    public static final String KOMPICS_EVENT = "KOMPICS_EVENT";
    public static final String RANDOM = "RANDOM";
            
    private static final Multimap<String, KDecorator> decorators = ArrayListMultimap.create();

    public static void register(String decoratorType, KDecorator decorator) {
        decorators.put(decoratorType, decorator);
    }

    public static void applyBefore(String decoratorType, String callingClass, Object[] args) {
        for (KDecorator decorator : decorators.get(decoratorType)) {
            decorator.applyBefore(callingClass, args);
        }
    }

    public static void applyAfter(String decoratorType, String callingClass, Object[] args) {
        for (KDecorator decorator : decorators.get(decoratorType)) {
            decorator.applyAfter(callingClass, args);
        }
    }
    
}
