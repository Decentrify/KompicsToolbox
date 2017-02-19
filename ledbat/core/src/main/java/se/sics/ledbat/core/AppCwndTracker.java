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
package se.sics.ledbat.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AppCwndTracker {

  private final BufferedWriter adjustmentFile;
  private final BufferedWriter rttFile;
  private final long start;
  private long lastReported = 0;

  public AppCwndTracker(BufferedWriter adjustmentFile, BufferedWriter rttFile) {
    this.adjustmentFile = adjustmentFile;
    this.rttFile = rttFile;
    this.start = System.currentTimeMillis();
  }

  public void reportAdjustment(long now, double adjustment, double appCwnd) {
    try {
      long expTime = (now - start)/1000;
      adjustmentFile.write(expTime + "," + adjustment + "," + appCwnd + "\n");
      adjustmentFile.flush();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void reportRTT(long now, long rtt) {
    if (now > lastReported + 1000) { //every 1s
      lastReported = now;
      try {
        long expTime = (now - start) / 1000;
        rttFile.write(expTime + "," + rtt + "\n");
        rttFile.flush();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public void close() {
    try {
      adjustmentFile.close();
      rttFile.close();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static AppCwndTracker onDisk(String dirPath, Identifier cwndId) {
    try {
      DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      Date date = new Date();
      String afName = "app_cwnd_a_" + cwndId.toString() + "_" + sdf.format(date) + ".csv";
      File af = new File(dirPath + File.separator + afName);
      if (af.exists()) {
        af.delete();
      }
      af.createNewFile();
      
      String rfName = "app_cwnd_r_" + cwndId.toString() + "_" + sdf.format(date) + ".csv";
      File rf = new File(dirPath + File.separator + rfName);
      if (rf.exists()) {
        rf.delete();
      }
      rf.createNewFile();
      return new AppCwndTracker(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(af))),
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rf))));
    } catch (FileNotFoundException ex) {
      throw new RuntimeException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
