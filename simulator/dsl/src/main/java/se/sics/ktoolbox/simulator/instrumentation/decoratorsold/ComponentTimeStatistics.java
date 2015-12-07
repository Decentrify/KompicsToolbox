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
package se.sics.ktoolbox.simulator.instrumentation.decoratorsold;

import java.util.HashMap;
import java.util.Map;
import se.sics.kompics.KompicsEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ComponentTimeStatistics {

    public final Class comp;
    public final Map<Class, HandlerTimeStatistics> executedHandlers = new HashMap<>();

    public ComponentTimeStatistics(Class comp) {
        this.comp = comp;
    }

    public void startExecuteEvent(Object handler, KompicsEvent event) {
        HandlerTimeStatistics hts = executedHandlers.get(handler.getClass());
        if (hts == null) {
            //TODO Alex turn to factory
            hts = new CompleteHandlerTimeStatistics(handler);
            executedHandlers.put(handler.getClass(), hts);
        }
        hts.startExecuteEvent(event);
    }

    public void endExecuteEvent(Object handler, KompicsEvent event) {
        HandlerTimeStatistics hts = executedHandlers.get(handler.getClass());
        assert hts != null;
        hts.endExecuteEvent(event);
    }
}
