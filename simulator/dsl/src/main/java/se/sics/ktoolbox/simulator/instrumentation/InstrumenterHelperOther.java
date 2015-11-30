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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.ktoolbox.simulator.SimulationScenario;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class InstrumenterHelperOther {
    private static Logger LOG = LoggerFactory.getLogger(CodeInstrumenter.class);

    private static final String directory = "./target/kompics-simulation/";
    private static HashSet<String> exceptions = new HashSet<String>();

    /**
     * Prepare instrumentation exceptions.
     */
    private static void prepareInstrumentationExceptions() {
        // well known exceptions
        exceptions.add("java.lang.ref.Reference");
        exceptions.add("java.lang.ref.Finalizer");
        exceptions.add("se.sics.kompics.p2p.simulator.P2pSimulator");
        exceptions.add("org.apache.log4j.PropertyConfigurator");
        exceptions.add("org.apache.log4j.helpers.FileWatchdog");
        exceptions.add("org.mortbay.thread.QueuedThreadPool");
        exceptions.add("org.mortbay.io.nio.SelectorManager");
        exceptions.add("org.mortbay.io.nio.SelectorManager$SelectSet");
        exceptions.add("org.apache.commons.math.stat.descriptive.SummaryStatistics");
        exceptions.add("org.apache.commons.math.stat.descriptive.DescriptiveStatistics");

        // try to add user-defined exceptions from properties file
        InputStream in = ClassLoader.getSystemResourceAsStream("timer.interceptor.properties");
        Properties p = new Properties();
        if (in != null) {
            try {
                p.load(in);
                for (String classname : p.stringPropertyNames()) {
                    String value = p.getProperty(classname);
                    if (value != null && value.equals("IGNORE")) {
                        exceptions.add(classname);
                        LOG.debug("Adding instrumentation exception {}", classname);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.debug("Could not find timer.interceptor.properties");
        }
    }

    private static void instrumentBoot(String bootString, boolean allowThreads) {
        String bootCp = System.getProperty("sun.boot.class.path");
        try {
            transformClasses(bootCp, bootString, allowThreads);
            copyResources(bootCp, bootString);
        } catch (Throwable t) {
            throw new RuntimeException(
                    "Exception caught while preparing simulation", t);
        }
    }

    /**
     * Instrument application.
     */
    private static void instrumentApplication(boolean allowThreads) {
        String cp = System.getProperty("java.class.path");
        try {
            transformClasses(cp, null, allowThreads);
            copyResources(cp, null);
        } catch (Throwable t) {
            throw new RuntimeException("Exception caught while preparing simulation", t);
        }
    }

    private static void transformClasses(String classPath, String boot, boolean allowThreads)
            throws IOException, NotFoundException, CannotCompileException {
        LinkedList<String> classes = getAllClasses(classPath);

        ClassPool pool = new ClassPool();
        pool.appendSystemPath();

        String target = directory;
        if (boot == null) {
            pool.appendPathList(classPath);
            target += "application";
            LOG.info("Instrumenting application classes to:" + target);
        } else {
            target += boot;
            LOG.info("Instrumenting bootstrap classes to:" + target);
        }
        CodeInstrumenter ci = new CodeInstrumenter(allowThreads, CodeInterceptor.DEFAULT_REDIRECT);

        int count = classes.size();
        long start = System.currentTimeMillis();

        for (final String classname : classes) {

            int d = classname.indexOf("$");
            String outerClass = (d == -1 ? classname : classname
                    .substring(0, d));

            CtClass ctc = pool.getCtClass(classname);

            if (!exceptions.contains(outerClass)) {
                ctc.defrost();
                ctc.instrument(ci);
            } else {
                LOG.trace("Skipping " + classname);
            }
            saveClass(ctc, target);
        }

        long stop = System.currentTimeMillis();
        LOG.info("It took " + (stop - start) + "ms to instrument " + count
                + " classes.");
    }

    /**
     * Already instrumented boot.
     *
     * @param bootString the boot string
     *
     * @return true, if successful
     */
    private static boolean alreadyInstrumentedBoot(String bootString) {
        File f = new File(directory + bootString);
        return f.exists() && f.isDirectory();
    }

    /**
     * Boot string.
     *
     * @return the string
     */
    private static String bootString() {
        String os = System.getProperty("os.name");
        int sp = os.indexOf(' ');
        if (sp != -1) {
            os = os.substring(0, sp);
        }
        String vendor = System.getProperty("java.vendor");
        sp = vendor.indexOf(' ');
        if (sp != -1) {
            vendor = vendor.substring(0, sp);
        }

        return "boot-" + vendor + "-" + System.getProperty("java.version")
                + "-" + os + "-" + System.getProperty("os.arch");
    }

    /**
     * Good env.
     *
     * @return true, if successful
     */
    private static boolean goodEnv() {
        if (System.getProperty("java.vendor").startsWith("Sun")) {
            return true;
        }
        if (System.getProperty("java.vendor").startsWith("Oracle")) {
            return true;
        }
        // we should change this method to accept more (or less) Java
        // environments known to be (un)acceptable for our instrumentation
        return false;
    }

    /**
     * Save class.
     *
     * @param cc the cc
     * @param dir the dir
     */
    private static void saveClass(CtClass cc, String dir) {
        File directory = new File(dir);
        if (directory != null) {
            try {
                cc.writeFile(directory.getAbsolutePath());
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copy resources.
     *
     * @param classPath the class path
     * @param boot the boot
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void copyResources(String classPath, String boot)
            throws IOException {
        LinkedList<String> resources = getAllResources(classPath);

        String target = directory;
        if (boot == null) {
            target += "application";
            LOG.info("Copying application resources to:" + target);
        } else {
            target += boot;
            LOG.info("Copying bootstrap resources to:" + target);
        }

        int count = resources.size();
        long start = System.currentTimeMillis();

        for (final String resourceName : resources) {
            InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(resourceName);

            String targetFile = target + "/" + resourceName;
            File dir = new File(new File(targetFile).getParent());
            if (!dir.exists()) {
                dir.mkdirs();
                dir.setWritable(true);
            }
            OutputStream os = new FileOutputStream(targetFile);
            byte buffer[] = new byte[65536];
            int len;

            long ms = System.currentTimeMillis();

            // copy the resource
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();

            ms = System.currentTimeMillis() - ms;
            LOG.trace("Copying " + resourceName + " to "
                    + (target + "/" + resourceName) + " - took " + ms + "ms.");
        }

        long stop = System.currentTimeMillis();
        LOG.info("It took " + (stop - start) + "ms to copy " + count
                + " resources.");
    }

    /**
     * Gets the all classes.
     *
     * @param cp the cp
     *
     * @return the all classes
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static LinkedList<String> getAllClasses(String cp) throws IOException {
        LinkedList<String> list = new LinkedList<String>();

        for (String location : getAllLocations(cp)) {
            list.addAll(getClassesFromLocation(location));
        }
        return list;
    }

    /**
     * Gets the all resources.
     *
     * @param cp the cp
     *
     * @return the all resources
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static LinkedList<String> getAllResources(String cp) throws IOException {
        LinkedList<String> list = new LinkedList<String>();

        for (String location : getAllLocations(cp)) {
            list.addAll(getResourcesFromLocation(location));
        }
        return list;
    }

    /**
     * Gets the all locations.
     *
     * @param cp the cp
     *
     * @return the all locations
     */
    private static LinkedList<String> getAllLocations(String cp) {
        LinkedList<String> list = new LinkedList<String>();

        for (String string : cp.split(System.getProperty("path.separator"))) {
            list.add(string);
        }
        return list;
    }

    /**
     * Gets the classes from location.
     *
     * @param location the location
     *
     * @return the classes from location
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static LinkedList<String> getClassesFromLocation(String location)
            throws IOException {
        File f = new File(location);

        if (f.exists() && f.isDirectory()) {
            return getClassesFromDirectory(f, "");
        }
        if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
            return getClassesFromJar(f);
        }

        LinkedList<String> list = new LinkedList<String>();
        return list;
    }

    /**
     * Gets the classes from jar.
     *
     * @param jar the jar
     *
     * @return the classes from jar
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static LinkedList<String> getClassesFromJar(File jar) throws IOException {
        JarFile j = new JarFile(jar);

        LinkedList<String> list = new LinkedList<String>();

        // System.err.println("Jar entries: " + j.size());
        Enumeration<JarEntry> entries = j.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                String className = entry.getName().substring(0,
                        entry.getName().lastIndexOf('.'));
                list.add(className.replace('/', '.'));
            }
            // System.err.println(entry);
        }
        return list;
    }

    /**
     * Gets the classes from directory.
     *
     * @param directory the directory
     * @param pack the pack
     *
     * @return the classes from directory
     */
    private static LinkedList<String> getClassesFromDirectory(File directory,
            String pack) {
        String[] files = directory.list();

        LinkedList<String> list = new LinkedList<String>();
        for (String string : files) {
            File f = new File(directory + System.getProperty("file.separator")
                    + string);

            if (f.isFile() && f.getName().endsWith(".class")) {
                String className = f.getName().substring(0,
                        f.getName().lastIndexOf('.'));
                list.add(pack + className);
            }

            if (f.isDirectory()) {
                LinkedList<String> classes = getClassesFromDirectory(f, pack
                        + f.getName() + ".");
                list.addAll(classes);
            }
        }
        return list;
    }

    /**
     * Gets the resources from location.
     *
     * @param location the location
     *
     * @return the resources from location
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static LinkedList<String> getResourcesFromLocation(String location)
            throws IOException {
        File f = new File(location);

        if (f.exists() && f.isDirectory()) {
            return getResourcesFromDirectory(f, "");
        }
        if (f.exists() && f.isFile() && f.getName().endsWith(".jar")) {
            return getResourcesFromJar(f);
        }

        LinkedList<String> list = new LinkedList<String>();
        return list;
    }

    /**
     * Gets the resources from jar.
     *
     * @param jar the jar
     *
     * @return the resources from jar
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static LinkedList<String> getResourcesFromJar(File jar) throws IOException {
        JarFile j = new JarFile(jar);

        LinkedList<String> list = new LinkedList<>();

        Enumeration<JarEntry> entries = j.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (!entry.getName().endsWith(".class") && !entry.isDirectory()
                    && !entry.getName().startsWith("META-INF")) {
                String resourceName = entry.getName();
                list.add(resourceName);
            }
        }
        return list;
    }

    /**
     * Gets the resources from directory.
     *
     * @param directory the directory
     * @param pack the pack
     *
     * @return the resources from directory
     */
    private static LinkedList<String> getResourcesFromDirectory(File directory,
            String pack) {
        String[] files = directory.list();

        LinkedList<String> list = new LinkedList<>();
        for (String string : files) {
            File f = new File(directory + System.getProperty("file.separator")
                    + string);

            if (f.isFile() && !f.getName().endsWith(".class")) {
                String resourceName = f.getName();
                list.add(pack + resourceName);
            }

            if (f.isDirectory()) {
                LinkedList<String> resources = getResourcesFromDirectory(f,
                        pack + f.getName() + "/");
                list.addAll(resources);
            }
        }
        return list;
    }

     public final void transform(SimulationScenario scenario, Class<? extends ComponentDefinition> main, String directory, boolean allowThreads) {
        Properties p = new Properties();

        File dir = null;
        File file = null;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            dir = new File(directory);
            dir.mkdirs();
            dir.setWritable(true);
            file = File.createTempFile("scenario", ".bin", dir);
            oos.writeObject(scenario);
            oos.flush();
            System.setProperty("scenario", file.getAbsolutePath());
            p.setProperty("scenario", file.getAbsolutePath());
            p.store(new FileOutputStream(file.getAbsolutePath() + ".properties"), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Loader cl = AccessController.doPrivileged(new PrivilegedAction<Loader>() {
                @Override
                public Loader run() {
                    return new Loader();
                }
            });
            cl.addTranslator(ClassPool.getDefault(), new CodeInterceptor(dir, allowThreads));
            Thread.currentThread().setContextClassLoader(cl);
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            cl.run(main.getCanonicalName(), null);
        } catch (Throwable e) {
            throw new RuntimeException("Exception caught during simulation", e);
        }
    }
     
    public final void execute(SimulationScenario scenario, Class<? extends ComponentDefinition> main) {
        InstrumentationHelper.store(scenario);
        try {
            Loader cl = AccessController.doPrivileged(new PrivilegedAction<Loader>() {
                @Override
                public Loader run() {
                    return new Loader();
                }
            });
            Thread.currentThread().setContextClassLoader(cl);
            cl.run(main.getCanonicalName(), null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
    public final void sim(SimulationScenario scenario, Class<? extends ComponentDefinition> main, boolean allowThreads, String... args) {
        InstrumentationHelper.store(scenario);

        // 1. validate environment: quit if not Sun
        if (!goodEnv()) {
            throw new RuntimeException("Only Sun JRE usable for simulation");
        }

        // 2. compute boot-string
        String bootString = bootString();

        // 3. check if it already exists; goto 5 if it does
        if (!alreadyInstrumentedBoot(bootString)) {
            // 4. transform and generate boot classes in boot-string directory
            prepareInstrumentationExceptions();
            instrumentBoot(bootString, allowThreads);
        } else {
            prepareInstrumentationExceptions();
        }

        // 5. transform and generate application classes
        instrumentApplication(allowThreads);

        // 6. launch simulation process
        launchSimulation(main, args);
    }

    public static void instrument(boolean allowThreads) {
        // 1. validate environment: quit if not Sun
        if (!goodEnv()) {
            throw new RuntimeException("Only Sun JRE usable for simulation");
        }

        // 2. compute boot-string
        String bootString = bootString();

        // 3. check if it already exists; goto 5 if it does
        if (!alreadyInstrumentedBoot(bootString)) {
            // 4. transform and generate boot classes in boot-string directory
            prepareInstrumentationExceptions();
            instrumentBoot(bootString, allowThreads);
        } else {
            prepareInstrumentationExceptions();
        }

        // 5. transform and generate application classes
        instrumentApplication(allowThreads);
    }
    
    private void launchSimulation(Class<? extends ComponentDefinition> main, String... args) {
        LinkedList<String> arguments = new LinkedList<>();

        String java = System.getProperty("java.home");
        String sep = System.getProperty("file.separator");
        String pathSep = System.getProperty("path.separator");
        java += sep + "bin" + sep + "java";

        if (System.getProperty("os.name").startsWith("Windows")) {
            arguments.add("\"" + java + "\"");
        } else {
            arguments.add(java);
        }

        arguments.add("-Xbootclasspath:" + directory + bootString() + pathSep + directory + "application");
        arguments.add("-classpath");
        arguments.add(directory + "application");
        arguments.addAll(getJvmArgs(args));
        arguments.add("-Dscenario=" + System.getProperty("scenario"));

        // add configuration properties
        for (Object key : System.getProperties().keySet()) {
            if (((String) key).contains("configuration")) {
                arguments.add("-D" + key + "=" + System.getProperty((String) key));
            }
        }

        arguments.add(main.getName());
        arguments.addAll(getApplicationArgs(args));

        ProcessBuilder pb = new ProcessBuilder(arguments);
        pb.redirectErrorStream(true);

        saveSimulationCommandLine(arguments);

        try {
            Process process = pb.start();
            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                do {
                    line = out.readLine();
                    if (line != null) {
                        System.out.println(line);
                    }
                } while (line != null);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot start simulation process", e);
        }
    }

    private static void saveSimulationCommandLine(final LinkedList<String> args) {
        try {
            // Windows batch file
            try (PrintStream ps = new PrintStream(new File(directory + "run-simulation.bat"))) {

                for (String arg : args) {
                    ps.println(arg.replaceAll(directory, "") + "\t^");
                    // ps.println(arg + "\t^");
                }
                ps.println(";");
                ps.flush();
            }

            // Linux/Unix Bash script
            try (PrintStream ps = new PrintStream(new File(directory + "run-simulation.sh"))) {
                ps.println("#!/bin/bash");
                ps.println();

                for (String arg : args) {
                    ps.println(arg.replaceAll(directory, "") + "\t\\");
                    // ps.println(arg + "\t\\");
                }
                ps.println(";");
                ps.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static LinkedList<String> getJvmArgs(String[] args) {
        LinkedList<String> list = new LinkedList<>();
        boolean maxHeap = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-JVM:")) {
                String a = args[i].substring(5);

                if (a.startsWith("-Xmx")) {
                    maxHeap = true;
                }
                if (a.startsWith("-Xbootclasspath") || a.startsWith("-cp")
                        || a.startsWith("-classpath")) {
                    continue; // ignore class-path settings
                }
                list.add(a);
            }
        }

        if (!maxHeap) {
            list.add("-Xmx1g");
        }
        return list;
    }

    private static LinkedList<String> getApplicationArgs(String[] args) {
        LinkedList<String> list = new LinkedList<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-JVM:")) {
                list.add(args[i]);
            }
        }
        return list;
    }
}
