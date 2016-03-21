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
package se.sics.ktoolbox.simulator.instrumentation.util;

import java.util.HashSet;
import java.util.Set;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.MatchedHandler;
import se.sics.kompics.PatternExtractor;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HandlerDecoratorRegistry {
    private static Set<HandlerDecorator> beforeHandlerDecorators = new HashSet<>();
    private static Set<HandlerDecorator> afterHandlerDecorators = new HashSet<>();
    
    public static void register(Set<HandlerDecorator> setBeforeHandlerDecorators, 
            Set<HandlerDecorator> setAfterHandlerDecorators) {
        beforeHandlerDecorators = setBeforeHandlerDecorators;
        afterHandlerDecorators = setAfterHandlerDecorators;
    }

    public static void beforeHandler(JavaComponent comp, KompicsEvent event, Handler<?> handler) {
        for(HandlerDecorator decorator : beforeHandlerDecorators) {
            decorator.beforeHandler(comp, event, handler);
        }
    }
    
     public static void beforeHandler(JavaComponent comp, PatternExtractor event, MatchedHandler handler) {
        for(HandlerDecorator decorator : beforeHandlerDecorators) {
            decorator.beforeHandler(comp, event, handler);
        }
    }

    public static void afterHandler(JavaComponent comp, KompicsEvent event, Handler<?> handler) {
        for(HandlerDecorator decorator : afterHandlerDecorators) {
            decorator.afterHandler(comp, event, handler);
        }
    }
    
    public static void afterHandler(JavaComponent comp, PatternExtractor event, MatchedHandler handler) {
        for(HandlerDecorator decorator : afterHandlerDecorators) {
            decorator.afterHandler(comp, event, handler);
        }
    }
}
