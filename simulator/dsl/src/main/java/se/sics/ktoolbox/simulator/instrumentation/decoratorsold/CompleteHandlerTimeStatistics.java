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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.KompicsEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CompleteHandlerTimeStatistics implements HandlerTimeStatistics {
    public final Object handler;
    public final Multimap<Long, Pair<KompicsEvent, Long>> executionTime = ArrayListMultimap.create();
    private Pair<Long, KompicsEvent> currentEvent;

    public CompleteHandlerTimeStatistics(Object handler) {
        this.handler = handler;
    }

    @Override
    public void startExecuteEvent(KompicsEvent event) {
        assert currentEvent == null;
        currentEvent = Pair.with(System.nanoTime(), event);
    }

    @Override
    public void endExecuteEvent(KompicsEvent event) {
        assert currentEvent != null;
        executionTime.put(currentEvent.getValue0(), Pair.with(currentEvent.getValue1(), System.nanoTime()));
        currentEvent = null;
    }
}
