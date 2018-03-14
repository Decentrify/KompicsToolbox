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
package se.sics.ktoolbox.httpsclient.hops;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Hopssite {

  public static class Target {

    public static String bbc5() {
      return "https://bbc5.sics.se:43080/hops-site/api";
    }
    
    public static String bbc5_test() {
      return "https://bbc5.sics.se:52300/hops-site/api";
    }
  }

  public static class Path {
    public static String search() {
      return "public/dataset/search";
    }
    
    public static String reportDataValues() {
    return "public/report/data";
  }
  
  public static String reportDownloadValues() {
    return "public/report/download";
  }
  }
}
