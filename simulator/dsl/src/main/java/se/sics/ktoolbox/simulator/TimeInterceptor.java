/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.ktoolbox.simulator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.Translator;

/**
 * The <code>TimeInterceptor</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class TimeInterceptor implements Translator {

    public static String TIME_INTERCEPTOR_EXCEPTIONS = "timer.interceptor.properties";
    public static String TIME_INTERCEPTOR_IGNORE = "IGNORE";
    public static final String DEFAULT_REDIRECT = SimulatorSystem.class.getName();

    private HashSet<String> exceptions = new HashSet<String>();

    private final File directory;
    private final boolean allowThreads;
    private final String redirect;

    public TimeInterceptor(File directory, boolean allowThreads, String redirect) {
        this.directory = directory;
        this.allowThreads = allowThreads;
        this.redirect = redirect;
    }

    public TimeInterceptor(File directory, boolean allowThreads) {
        this(directory, allowThreads, DEFAULT_REDIRECT);
    }

    @Override
    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
        registerKnownExceptions();
        registerUserDefinedExceptions();
    }

    private void registerKnownExceptions() {
        //well known exceptions
        exceptions.add("org.apache.log4j.PropertyConfigurator");
        exceptions.add("org.apache.log4j.helpers.FileWatchdog");
        exceptions.add("org.mortbay.thread.QueuedThreadPool");
        exceptions.add("org.mortbay.io.nio.SelectorManager");
        exceptions.add("org.mortbay.io.nio.SelectorManager$SelectSet");
        exceptions.add("org.apache.commons.math.stat.descriptive.SummaryStatistics");
        exceptions.add("org.apache.commons.math.stat.descriptive.DescriptiveStatistics");
    }

    private void registerUserDefinedExceptions() {
        // try to add user-defined exceptions from properties file
        InputStream in = ClassLoader.getSystemResourceAsStream(TIME_INTERCEPTOR_EXCEPTIONS);
        Properties p = new Properties();
        if (in != null) {
            try {
                p.load(in);
                for (String classname : p.stringPropertyNames()) {
                    String value = p.getProperty(classname);
                    if (value != null && value.equals(TIME_INTERCEPTOR_IGNORE)) {
                        exceptions.add(classname);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onLoad(ClassPool pool, final String classname) throws NotFoundException, CannotCompileException {
        int d = classname.indexOf("$");
        String outerClass = (d == -1 ? classname : classname.substring(0, d));

        if (exceptions.contains(outerClass)) {
            CtClass cc = pool.get(classname);
            saveClass(cc);
            return;
        }

        CtClass cc = pool.get(classname);
        cc.defrost();
        cc.instrument(new CodeInstrumenter(allowThreads, redirect));
        saveClass(cc);
    }

    private void saveClass(CtClass cc) {
        if (directory != null) {
            try {
                cc.writeFile(directory.getAbsolutePath());
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
