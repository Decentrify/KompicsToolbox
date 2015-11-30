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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.HashSet;
import java.util.Set;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CodeInstrumentation {

    private static final Logger LOG = LoggerFactory.getLogger("CodeInstrumentation");
    
    public static final String INTERCEPTOR_EXCEPTIONS = "instrumentation.exceptions";
    public static final String HANDLER_DECORATORS = "instrumentation.handler.decorators";
    public static final String HANDLER_DECORATOR_BEFORE = "before";
    public static final String HANDLER_DECORATOR_AFTER = "after";

    public static Set<String> knownInterceptorExceptions() {
        Set<String> exceptions = new HashSet<>();
        exceptions.add("org.apache.log4j.PropertyConfigurator");
        exceptions.add("org.apache.log4j.helpers.FileWatchdog");
        exceptions.add("org.mortbay.thread.QueuedThreadPool");
        exceptions.add("org.mortbay.io.nio.SelectorManager");
        exceptions.add("org.mortbay.io.nio.SelectorManager$SelectSet");
        exceptions.add("org.apache.commons.math.stat.descriptive.SummaryStatistics");
        exceptions.add("org.apache.commons.math.stat.descriptive.DescriptiveStatistics");
        return exceptions;
    }
    
    public static Set<String> readInterceptorExceptionsFromTypesafeConfig() {
        Config config = ConfigFactory.load();
        Set<String> exceptions = new HashSet<>(config.getStringList(INTERCEPTOR_EXCEPTIONS));
        return exceptions;
    }

//    public static Pair<Set<String>, Set<String>> readHandlerDecoratorsFromTypesafeConfig() {
//        Config config = ConfigFactory.load();
//        Set<String> beforeHandler = new HashSet<>(config.getStringList(HANDLER_DECORATORS + "." + HANDLER_DECORATOR_BEFORE));
//        Set<String> afterHandler = new HashSet<>(config.getStringList(HANDLER_DECORATORS + "." + HANDLER_DECORATOR_AFTER));
//        return Pair.with(beforeHandler, afterHandler);
//    }
}
