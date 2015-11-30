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
package se.sics.ktoolbox.simulator.instrumentation.other;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.ktoolbox.simulator.instrumentation.util.HandlerDecorator;
import se.sics.ktoolbox.simulator.instrumentation.util.HandlerDecorator1;
import se.sics.ktoolbox.simulator.instrumentation.util.HandlerDecorator2;
import se.sics.ktoolbox.simulator.instrumentation.util.HandlerDecorator3;
import se.sics.ktoolbox.simulator.instrumentation.util.HandlerDecoratorRegistry;
import se.sics.ktoolbox.simulator.instrumentation.util.Redirect;
import se.sics.ktoolbox.simulator.instrumentation.util.JavaComponent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CodeInstrumentationTest {

    @BeforeClass
    public static void before() {
        System.setProperty("config.file", "./src/test/resources/instrumentation.conf");
    }

    @Test
    public void basicTest() throws NotFoundException, CannotCompileException {
        ClassPool cp = new ClassPool();
        cp.insertClassPath(new ClassClassPath(JavaComponent.class));
        cp.insertClassPath(new ClassClassPath(HandlerDecoratorRegistry.class));
        cp.insertClassPath(new ClassClassPath(HandlerDecorator1.class));
        cp.insertClassPath(new ClassClassPath(HandlerDecorator2.class));
        cp.insertClassPath(new ClassClassPath(HandlerDecorator3.class));

        CodeInterceptor ci = new CodeInterceptor(null, false, Redirect.class.getName());
        ci.start(cp);

        System.out.println("Exceptions:");
        System.out.println(ci.printExceptions());
        System.out.println("***********");
        try {
            Loader cl = AccessController.doPrivileged(new PrivilegedAction<Loader>() {
                @Override
                public Loader run() {
                    return new Loader();
                }
            });
            cl.addTranslator(cp, ci);
            Thread.currentThread().setContextClassLoader(cl);
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            cl.run(TestMain.class.getName(), null);
        } catch (Throwable e) {
            throw new RuntimeException("Exception caught during simulation", e);
        }
    }

    public static class TestMain {

        public static void main(String[] args) {
            systemSetup();

            JavaComponent testC = new JavaComponent();
            testC.executeEvent1(null, null);
            testC.executeEvent2(null, null);
        }

        private static void systemSetup() {
            HandlerDecorator1 hd1 = new HandlerDecorator1();
            HandlerDecorator2 hd2 = new HandlerDecorator2();
            HandlerDecorator3 hd3 = new HandlerDecorator3();
            Set<HandlerDecorator> beforeDecorators = new HashSet<>();
            beforeDecorators.add(hd1);
            beforeDecorators.add(hd2);
            Set<HandlerDecorator> afterDecorators = new HashSet<>();
            afterDecorators.add(hd2);
            afterDecorators.add(hd3);
            HandlerDecoratorRegistry.register(beforeDecorators, afterDecorators);
        }
    }
}
