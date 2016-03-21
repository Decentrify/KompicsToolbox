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

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.Translator;
import org.slf4j.Logger;
import se.sics.kompics.Handler;
import se.sics.kompics.JavaComponent;
import se.sics.kompics.KompicsEvent;

/**
 * The <code>CodeInterceptor</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class CodeInterceptor implements Translator {

    private static final Logger LOG = CodeInstrumentation.INSTRUMENTATION_LOG;
    public static final String DEFAULT_REDIRECT = SimulatorSystem.class.getName();

    private final File directory;
    private final boolean allowThreads;
    private final String redirect;

    public CodeInterceptor(File directory, boolean allowThreads, String redirect) {
        this.directory = directory;
        this.allowThreads = allowThreads;
        this.redirect = redirect;
    }

    public CodeInterceptor(File directory, boolean allowThreads) {
        this(directory, allowThreads, DEFAULT_REDIRECT);
    }

    @Override
    public void start(ClassPool cp) throws NotFoundException, CannotCompileException {
    }

    @Override
    public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
        if (isException(pool, classname)) {
            return;
        }

        CtClass cc = pool.get(classname);
        cc.defrost();
        
//        if (JavaComponent.class.getName().equals(classname)) {
//            decorateHandlers(pool, cc);
//        }
        cc.instrument(new BaseEditor(redirect, allowThreads));
        saveClass(cc);
    }

    private boolean isException(ClassPool pool, final String classname) {
        StringTokenizer st = new StringTokenizer(classname, "$");
        String auxClassname = null;
        while (st.hasMoreTokens()) {
            auxClassname = (auxClassname == null ? st.nextToken() : auxClassname + "$" + st.nextToken());
            if (CodeInstrumentation.instrumentationExceptedClass.contains(auxClassname)) {
                return true;
            }
        }
        return false;
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
    
//    private void decorateHandlers(ClassPool pool, CtClass cc) throws NotFoundException, CannotCompileException {
//
//        //decorate simple handler
//        CtClass[] simple = new CtClass[]{
//            pool.get(KompicsEvent.class.getName()),
//            pool.get(Handler.class.getName())};
//        CtMethod simpleHandler = cc.getDeclaredMethod("executeEvent", simple);
//
//        simpleHandler.insertBefore(
//                "{ " + 
//                        HandlerDecorators.class.getName() + ".beforeHandler($0, $1, $2); }");
//        simpleHandler.insertAfter(
//                "{ " + HandlerDecorators.class.getName() + ".afterHandler($0, $1, $2); }");
//
//        CtClass[] pattern = new CtClass[]{
//            pool.get(PatternExtractor.class.getName()),
//            pool.get(MatchedHandler.class.getName())};
//        CtMethod patternHandler = cc.getDeclaredMethod("executeEvent", pattern);
//
//        patternHandler.insertBefore(
//                "{ " + HandlerDecoratorRegistry.class.getName() + ".beforeHandler($0, $1, $2); }");
//        patternHandler.insertAfter(
//                "{ " + HandlerDecoratorRegistry.class.getName() + ".afterHandler($0, $1, $2); }");
//    }

}
