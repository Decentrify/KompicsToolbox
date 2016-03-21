///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.simulator.instrumentation.decorators;
//
//import java.security.AccessController;
//import java.security.PrivilegedAction;
//import java.security.SecureRandom;
//import java.util.Random;
//import java.util.TimeZone;
//import java.util.UUID;
//import javassist.CannotCompileException;
//import javassist.ClassClassPath;
//import javassist.ClassPool;
//import javassist.Loader;
//import javassist.NotFoundException;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import se.sics.ktoolbox.simulator.instrumentation.CodeInstrumentation;
//import se.sics.ktoolbox.simulator.instrumentation.CodeInterceptor;
//import se.sics.ktoolbox.simulator.instrumentation.SimulatorSystem;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class BasicKDecoratorTest {
//
//    @BeforeClass
//    public static void before() {
//        System.setProperty("config.file", "./src/test/resources/instrumentation.conf");
//    }
//
//    @Test
//    public void basicTest() throws NotFoundException, CannotCompileException {
//        ClassPool cp = ClassPool.getDefault();
//        cp.appendClassPath(new ClassClassPath(Random.class));
//        
//        CodeInterceptor ci = new CodeInterceptor(null, false);
//        ci.start(cp);
//
//        try {
//            Loader cl = AccessController.doPrivileged(new PrivilegedAction<Loader>() {
//                @Override
//                public Loader run() {
//                    return new Loader();
//                }
//            });
//            cl.addTranslator(cp, ci);
//            Thread.currentThread().setContextClassLoader(cl);
//            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
//            cl.run(TestMain.class.getName(), null);
//        } catch (Throwable e) {
//            throw new RuntimeException("Exception caught during simulation", e);
//        }
//    }
//
//    public static class TestMain {
//
//        public static void main(String[] args) {
//            //simulation setup done by simulator launcher
//            SimulatorSystem.setSimulatorRandomGen(new Random(123));
//            //simulation setup done by simulator launcher - access to stubs here
//            StubRandomDecorator stubRandomDecorator = new StubRandomDecorator();
//            CodeInstrumentation.register(Random.class.getName(), stubRandomDecorator);
//            
//            Random rand = new Random();
//            Assert.assertEquals(-1155484576, rand.nextInt());
//            Random rand1 = new Random(123);
//            Assert.assertEquals(-1188957731, rand1.nextInt());
//            
//            Assert.assertEquals(1, stubRandomDecorator.beforeNoArgConstructor);
//            Assert.assertEquals(1, stubRandomDecorator.beforeSeedConstructor);
//            
//            Assert.assertEquals("b921f1dd-3cbc-0495-fdab-8cd14d33f0aa", UUID.randomUUID().toString());
//            Assert.assertEquals("40d7d115-92fa-2632-9bda-574542460f3a", UUID.randomUUID().toString());
//            
//            SecureRandom srand = new SecureRandom(new byte[]{1,2,3,4});
//            System.out.println(srand.nextInt());
//            SecureRandom srand2 = new SecureRandom(new byte[]{1,2,3,4});
//            System.out.println(srand2.nextInt());
//        }
//    }
//}
