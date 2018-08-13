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
package se.sics.ktoolbox.util.classloader;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * https://gualtierotesta.wordpress.com/2015/07/05/java-how-to-check-jar-version-at-runtime/
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ClassLoaderHelper {

  public static String loadedBy(PrintWriter out, Class targetClass) throws MalformedURLException, IOException {
    // Find the path of the compiled class 
    String classPath = targetClass.getResource(targetClass.getSimpleName() + ".class").toString();
    out.printf("class:%s" + classPath);

    // Find the path of the lib which includes the class 
    String libPath = classPath.substring(0, classPath.lastIndexOf("!"));
    System.out.println("Lib:   " + libPath);

    // Find the path of the file inside the lib jar 
    String filePath = libPath + "!/META-INF/MANIFEST.MF";
    out.println("File:  " + filePath);
    Manifest manifest = new Manifest(new URL(filePath).openStream());
    Attributes attr = manifest.getMainAttributes();
    out.println("Manifest-Version: " + attr.getValue("Manifest-Version"));
    out.println("Implementation-Version: " + attr.getValue("Implementation-Version"));
    return libPath;
  }
}
