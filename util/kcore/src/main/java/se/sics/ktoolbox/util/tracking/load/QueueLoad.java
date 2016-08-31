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
package se.sics.ktoolbox.util.tracking.load;

import java.util.Random;
import se.sics.ktoolbox.util.tracking.load.util.FuzzyState;
import se.sics.ktoolbox.util.tracking.load.util.StatusState;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class QueueLoad {
    //**************************************************************************
    private final QueueLoadConfig loadConfig;
    //**************************************************************************
    private long checkPeriod;
    private MovingAvg delay;
    private final FuzzyState fuzzyState;
    private StatusState state;
    
    public QueueLoad(QueueLoadConfig loadConfig) {
        this.loadConfig = loadConfig;
        double targetLoad = (double) loadConfig.targetQueueDelay / loadConfig.maxQueueDelay;
        this.fuzzyState = new FuzzyState(targetLoad, new Random(loadConfig.seed));
        this.checkPeriod = loadConfig.maxQueueDelay;
        this.delay = new MovingAvg(0, 0);
        this.state = StatusState.MAINTAIN;
    }
    
    public StatusState state() {
        return state;
    }
    
    public long nextCheckPeriod() {
        return checkPeriod;
    }
    
    public void adjustState(long queueDelay) {
        delay.update(queueDelay);

        StatusState old = state;
        if (delay.get() > loadConfig.maxQueueDelay) {
            state = StatusState.SLOW_DOWN;
        } else {
            state = fuzzyState.state(delay.get() / loadConfig.maxQueueDelay);
        }
        
        if (!old.equals(state)) {
            checkPeriod = loadConfig.maxQueueDelay;
        } else {
            checkPeriod = 2 * loadConfig.maxQueueDelay;
        }
    }
    
    public long queueDelay() {
        return (long)delay.get();
    }
}
