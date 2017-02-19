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
package se.sics.nutil.tracking.load;

import com.google.common.base.Optional;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.config.Config;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.ports.ChannelFilter;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkQueueLoadProxy {

  private final static Logger LOG = LoggerFactory.getLogger(NetworkQueueLoadProxy.class);
  private String logPrefix = "";

  private static final ChannelFilter filter = new ChannelFilter() {

    @Override
    public boolean filter(KompicsEvent event) {
      if (!(event instanceof KContentMsg)) {
        return false;
      }
      KContentMsg msg = (KContentMsg) event;
      if (msg.getContent() instanceof LoadTrackingEvent) {
        return true;
      }
      return false;
    }
  };

  private final ComponentProxy proxy;
  private final QueueLoad loadTracker;
  private double adjustment;
  private final Optional<BufferedWriter> loadFile;
  private final long start;

  public NetworkQueueLoadProxy(String queueName, ComponentProxy proxy, QueueLoadConfig loadConfig,
    Optional<BufferedWriter> loadFile) {
    this.proxy = proxy;
    loadTracker = new QueueLoad(loadConfig);
    adjustment = 0.0;
    logPrefix = queueName;
    this.loadFile = loadFile;
    proxy.subscribe(handleTrackingTimeout, proxy.getNegative(Timer.class).getPair());
    proxy.subscribe(handleTrackingMsg, proxy.getNegative(Network.class).getPair());
    this.start = System.currentTimeMillis();
  }

  public void start() {
    scheduleLoadCheck();
  }

  public void tearDown() {
  }

  public ChannelFilter getFilter() {
    return filter;
  }

  public double adjustment() {
    return adjustment;
  }

  public Pair<Integer, Integer> queueDelay() {
    return loadTracker.queueDelay();
  }

  Handler handleTrackingTimeout = new Handler<LoadTrackingTimeout>() {

    @Override
    public void handle(LoadTrackingTimeout event) {
      KHeader header = new BasicHeader(null, null, null);
      KContentMsg msg = new BasicContentMsg(header, new LoadTrackingEvent());
      proxy.trigger(msg, proxy.getNegative(Network.class));
    }
  };

  ClassMatchedHandler handleTrackingMsg
    = new ClassMatchedHandler<LoadTrackingEvent, KContentMsg<KAddress, KHeader<KAddress>, LoadTrackingEvent>>() {
      @Override
      public void handle(LoadTrackingEvent content, KContentMsg<KAddress, KHeader<KAddress>, LoadTrackingEvent> context) {
        long now = System.currentTimeMillis();
        int queueDelay = (int) (now - content.sentAt);
        adjustment = loadTracker.adjustState(queueDelay);
        LOG.info("{}component adjustment:{} qd:{} avg qd:{}", new Object[]{logPrefix, adjustment, queueDelay,
          loadTracker.queueDelay()});
        reportLoad(now, queueDelay);
        if (adjustment < -0.8) {
          LOG.warn("{}overloaded", logPrefix);
        }
        scheduleLoadCheck();
      }
    };

  private void scheduleLoadCheck() {
    ScheduleTimeout st = new ScheduleTimeout(loadTracker.nextCheckPeriod());
    LoadTrackingTimeout ltt = new LoadTrackingTimeout(st);
    st.setTimeoutEvent(ltt);
    proxy.trigger(st, proxy.getNegative(Timer.class).getPair());
  }

  public void reportLoad(long now, long load) {
    if (loadFile.isPresent()) {
      try {
        double expTime = (now - start)/1000;
        loadFile.get().write(expTime + "," + load + "\n");
        loadFile.get().flush();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public static NetworkQueueLoadProxy instance(String name, ComponentProxy proxy, Config config,
    Optional<String> reportDir) {
    Optional<BufferedWriter> loadF = Optional.absent();
    if (reportDir.isPresent()) {
      loadF = Optional.fromNullable(onDiskLoadTracker(reportDir.get(), name));
    }
    return new NetworkQueueLoadProxy(name, proxy, new QueueLoadConfig(config), loadF);
  }

  private static BufferedWriter onDiskLoadTracker(String dirPath, String name) {
    try {
      DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      Date date = new Date();
      String loadfName = name + "_" + sdf.format(date) + ".csv";
      File loadf = new File(dirPath + File.separator + loadfName);
      if (loadf.exists()) {
        loadf.delete();
      }
      loadf.createNewFile();

      return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(loadf)));
    } catch (FileNotFoundException ex) {
      throw new RuntimeException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static class LoadTrackingTimeout extends Timeout {

    public LoadTrackingTimeout(ScheduleTimeout st) {
      super(st);
    }
  }
}
