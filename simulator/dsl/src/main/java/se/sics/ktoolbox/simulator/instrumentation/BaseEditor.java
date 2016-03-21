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

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import org.slf4j.Logger;
import se.sics.ktoolbox.simulator.SimulationScenario;

/**
 * The <code>BaseEditor</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class BaseEditor extends ExprEditor {

    private static final Logger LOG = CodeInstrumentation.INSTRUMENTATION_LOG;

    private final String redirect;
    private final boolean allowThreads;

    public BaseEditor(String redirect, boolean allowThreads) {
        this.redirect = redirect;
        this.allowThreads = allowThreads;
    }

    @Override
    public void edit(NewExpr newExpr) {
        String constructorClass = newExpr.getClassName();
        try {
            CtClass callingClass = newExpr.getEnclosingClass();
            CtClass[] parameters = newExpr.getConstructor().getParameterTypes();

            if (Random.class.getName().equals(constructorClass)) {

                String proceed = "$_ = $proceed($$);";
                if (parameters.length == 0) {
                    proceed = "$_ = $proceed(0l);";
                }

                newExpr.replace("{" + proceed + "}");
//                newExpr.replace("{ String instrumentationCallingClass = \"" + callingClass.getName() + "\"; "
//                        + "String instrumentationLoggerType = \"" + Random.class.getName() + "\";"
//                        + CodeInstrumentation.class.getName() + ".applyBefore(instrumentationLoggerType, instrumentationCallingClass, $args);"
//                        + proceed + " }");
                return;
            }
            if(SecureRandom.class.getName().equals(constructorClass)) {
                System.out.println("blas");
            }
        } catch (NotFoundException | CannotCompileException ex) {
            LOG.error("instrumentation of:{} error:{}", new Object[]{constructorClass, ex.getMessage()});
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void edit(MethodCall m) throws CannotCompileException {
        String callerClassName = m.getFileName();
        String className = m.getClassName();
        String method = m.getMethodName();
        if (className == null || method == null) {
            return;

        }

        //redirect random sources
        if (UUID.class
                .getName().equals(className) && method.equals("randomUUID")) {
            m.replace(
                    "{ long instrumentationUUIDLong1 = " + SimulationScenario.class.getName() + ".getRandom().nextLong(); "
                    + "long instrumentationUUIDLong2 = " + SimulationScenario.class.getName() + ".getRandom().nextLong(); "
                    + "$_ = new " + UUID.class
                    .getName() + "(instrumentationUUIDLong1, instrumentationUUIDLong2); }");
        }

        // redirect time sources
        if (className.equals(
                "java.lang.System") && method.equals("currentTimeMillis")) {
            m.replace("{ $_ = " + redirect + ".currentTimeMillis(); }");
            return;
        }

        if (className.equals(
                "java.lang.System") && method.equals("nanoTime")) {
            m.replace("{ $_ = " + redirect + ".nanoTime(); }");
            return;
        }

        //redirect thread sources
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

        // other
        if (className.equals(
                "java.util.TimeZone") && method.equals("getDefaultRef")) {
            m.replace("{ $_ = " + redirect + ".getDefaultTimeZone(); }");
            return;
        }
        SecureRandom r;

        //TODO Alex - old - doesn't work anymore
        if (className.equals(
                "java.security.SecureRandom") && method.equals("getPrngAlgorithm")) {
            m.replace("{ $_ = null; }");
            // System.err.println("REPLACED SECURE_RANDOM");
            return;
        }
    }
}
