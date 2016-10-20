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

import org.javatuples.Pair;
import se.sics.ktoolbox.util.predict.ExpMovingAvg;
import se.sics.ktoolbox.util.predict.STGMAvg;
import se.sics.ktoolbox.util.predict.SimpleSmoothing;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class QueueLoad {

    //**************************************************************************

    private final QueueLoadConfig loadConfig;
    private static final double IDLE = 0.1;
    //**************************************************************************
    private final STGMAvg avgQueueDelay;
    private int instQueueDelay;
    private long checkPeriod;

    public QueueLoad(QueueLoadConfig loadConfig) {
        this.loadConfig = loadConfig;
        this.avgQueueDelay = new STGMAvg(new ExpMovingAvg(), Pair.with(0.0, (double)loadConfig.targetQueueDelay), new SimpleSmoothing());
        this.instQueueDelay = 0;
        this.checkPeriod = loadConfig.maxQueueDelay;
    }

    public long nextCheckPeriod() {
        return checkPeriod;
    }

    public double adjustState(int queueDelay) {
        double adjustment = avgQueueDelay.update(queueDelay);
        this.instQueueDelay = queueDelay;
        if (avgQueueDelay.get() < IDLE * loadConfig.targetQueueDelay) {
            checkPeriod = loadConfig.maxQueueDelay;
        } else {
            checkPeriod = loadConfig.targetQueueDelay;
        }
        
        return adjustment;
    }

    public Pair<Integer, Integer> queueDelay() {
        return Pair.with((int)avgQueueDelay.get(), instQueueDelay);
    }
}
