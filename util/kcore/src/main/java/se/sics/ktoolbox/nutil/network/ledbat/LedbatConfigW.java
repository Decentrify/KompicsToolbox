/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.nutil.network.ledbat;

import com.google.common.base.Optional;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.trysf.Try;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatConfigW {
  public static final String BUFFER_SIZE = "ledbat.buffer.size";
  public static final String REPORT_PERIOD = "ledbat.report.period";
  
  public final LedbatConfig base;
  public final int statusPeriod = 100;
  public final long timerWindowSize;
  public final long maxTimeout = 25000;
  
  public final int bufferSize;
  public final Optional<Long> reportPeriod;

  public LedbatConfigW(LedbatConfig base, long timerWindowSize, int bufferSize, Optional<Long> reportPeriod) {
    this.base = base;
    this.timerWindowSize = timerWindowSize;
    this.bufferSize = bufferSize;
    this.reportPeriod = reportPeriod;
  }

  public static Try<LedbatConfigW> instance(Config config) {
    Try<LedbatConfig> base = LedbatConfig.instance(config);
    if(base.isFailure()) {
      return (Try.Failure)base;
    }
    Optional<Integer> bufferSize = config.readValue(BUFFER_SIZE, Integer.class);
    if(!bufferSize.isPresent()) {
      String msg = "missing value - " + BUFFER_SIZE;
      return new Try.Failure(new IllegalStateException(msg));
    }
    Optional<Long> reportPeriod = config.readValue(REPORT_PERIOD, Long.class);
    
    long timerWindowSize = base.get().MIN_RTO;
    return new Try.Success(new LedbatConfigW(base.get(), timerWindowSize, bufferSize.get(), reportPeriod));
  }
}
