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
 * along with this program; if not, loss to the Free Software
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
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import se.sics.kompics.id.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatCwndTracker {

  private final BufferedWriter cwndFile;
  private final BufferedWriter lossFile;
  private final BufferedWriter qdFile;
  private final long start;
  //********************************
  private long lastReported;
  private final SummaryStatistics cwndS = new SummaryStatistics();
  private final SummaryStatistics qdS = new SummaryStatistics();

  public LedbatCwndTracker(BufferedWriter cwndFile, BufferedWriter lossFile, BufferedWriter qdFile) {
    this.cwndFile = cwndFile;
    this.lossFile = lossFile;
    this.qdFile = qdFile;
    start = System.currentTimeMillis();
    lastReported = start;
    resetS();
  }

  private void resetS() {
    cwndS.clear();
    qdS.clear();
  }

  public void normal(long now, double cwndSize, long queuingDelay) {
    if (now > lastReported + 1000) { //every 1s
      lastReported = now;

      try {
        long expTime = (now - start) / 1000;
        cwndFile.write(expTime + "," + cwndS.getMean() + "," + cwndS.getMin() + "," + cwndS.getMax() + "\n");
        cwndFile.flush();
        qdFile.write(expTime + "," + qdS.getMean() + "," + qdS.getMin() + "," + qdS.getMax() + "\n");
        qdFile.flush();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      resetS();
    }
    cwndS.addValue(cwndSize);
    qdS.addValue(queuingDelay);
  }

  public void loss(long now, double cwndSize) {
    try {
      long expTime = (now - start) / 1000;
      lossFile.write(expTime + "," + cwndSize + "\n");
      lossFile.flush();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void close() {
    try {
      cwndFile.close();
      lossFile.close();
      qdFile.close();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static LedbatCwndTracker onDisk(String dirPath, Identifier id) {
    try {
      DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      Date date = new Date();
      String cwndfName = "cwnd_ledbat_" + id.toString() + "_" + sdf.format(date) + ".csv";
      File cwndf = new File(dirPath + File.separator + cwndfName);
      if (cwndf.exists()) {
        cwndf.delete();
      }
      cwndf.createNewFile();

      String lossfName = "loss_ledbat_" + id.toString() + "_" + sdf.format(date) + ".csv";
      File lossf = new File(dirPath + File.separator + lossfName);
      if (lossf.exists()) {
        lossf.delete();
      }
      lossf.createNewFile();

      String qdfName = "qd_ledbat" + id.toString() + "_" + sdf.format(date) + ".csv";
      File qdf = new File(dirPath + File.separator + qdfName);
      if (qdf.exists()) {
        qdf.delete();
      }
      qdf.createNewFile();
      
      return new LedbatCwndTracker(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cwndf))),
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lossf))),
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(qdf))));
    } catch (FileNotFoundException ex) {
      throw new RuntimeException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
