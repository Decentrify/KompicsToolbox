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
package se.sics.ktoolbox.util.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.PortType;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
//TODO Alex - do I need to make it thread safe? more than just sync methods?
public class PortRegistry {
    private static final Map<Class<? extends PortType>, Set<Class<? extends KompicsEvent>>> positiveEvents = new HashMap<>();
    private static final Map<Class<? extends PortType>, Set<Class<? extends KompicsEvent>>> negativeEvents = new HashMap<>();
   
    
    public static void registerPositive(Class<? extends KompicsEvent> eventClass, Class<? extends PortType> portType) {
        Set<Class<? extends KompicsEvent>> positivePortEv = positiveEvents.get(portType);
        if(positivePortEv == null) {
            positivePortEv = new HashSet<>();
            positiveEvents.put(portType, positivePortEv);
        }
        positivePortEv.add(eventClass);
    }
    
    public static Set<Class<? extends KompicsEvent>> getPositive(Class<? extends PortType> portType) {
        Set<Class<? extends KompicsEvent>> positivePortEv = positiveEvents.get(portType);
        if(positivePortEv == null) {
            return new HashSet<>();
        }
        return positivePortEv;
    }
    
    public static void registerNegative(Class<? extends KompicsEvent> eventClass, Class<? extends PortType> portType) {
        Set<Class<? extends KompicsEvent>> negativePortEv = negativeEvents.get(portType);
        if(negativePortEv == null) {
            negativePortEv = new HashSet<>();
            negativeEvents.put(portType, negativePortEv);
        }
        negativePortEv.add(eventClass);
    }
    
    public static Set<Class<? extends KompicsEvent>> getNegative(Class<? extends PortType> portType) {
        Set<Class<? extends KompicsEvent>> negativePortEv = negativeEvents.get(portType);
        if(negativePortEv == null) {
            return new HashSet<>();
        }
        return negativePortEv;
    }
}
