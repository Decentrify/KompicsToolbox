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
package se.sics.ktoolbox.simulator.instrumentation;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.JavaComponent;

/**
 * The <code>CodeInstrumenter</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class CodeInstrumenter extends ExprEditor {
    private static final Logger LOG = LoggerFactory.getLogger(CodeInstrumenter.class);

    private final boolean allowThreads;
    private final String redirect;

    public CodeInstrumenter(boolean allowThreads, String redirect) {
        this.allowThreads = allowThreads;
        this.redirect = redirect;
    }

    // redirect method calls
    @Override
    public void edit(MethodCall m) throws CannotCompileException {
        String callerClassName = m.getFileName();
        String className = m.getClassName();
        String method = m.getMethodName();
        try {
            // this traverses the class hierarchy up
            className = m.getMethod().getDeclaringClass().getName();
        } catch (NotFoundException e) {
            LOG.debug("Cannot instrument call to:{}.{}() in {} {}",
                    new Object[]{className, method, callerClassName, e});
            throw new RuntimeException(e);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        if (className == null || method == null) {
            return;
        }
        // redirect calls to System.currentTimeMillis()
        if (className.equals("java.lang.System") && method.equals("currentTimeMillis")) {
            m.replace("{ $_ = " + redirect + ".currentTimeMillis(); }");
            return;
        }
        // redirect calls to System.nanoTime()
        if (className.equals("java.lang.System") && method.equals("nanoTime")) {
            m.replace("{ $_ = " + redirect + ".nanoTime(); }");
            return;
        }

        if (!allowThreads) {
            // redirect calls to Thread.sleep()
            if (className.equals("java.lang.Thread") && method.equals("sleep")) {
                m.replace("{ " + redirect + ".sleep($$); }");
                return;
            }
            // redirect calls to Thread.start()
            if (className.equals("java.lang.Thread") && method.equals("start")) {
                m.replace("{ " + redirect + ".start(); }");
                return;
            }
        }
        // make sure the SHA1PRNG is used in SecureRandom
        if (className.equals("java.security.SecureRandom") && method.equals("getPrngAlgorithm")) {
            m.replace("{ $_ = null; }");
            // System.err.println("REPLACED SECURE_RANDOM");
            return;
        }
        // redirect calls to TimeZone.getDefault()
        if (className.equals("java.util.TimeZone") && method.equals("getDefaultRef")) {
            m.replace("{ $_ = " + redirect + ".getDefaultTimeZone(); }");
            return;
        }
    }
}
