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
package se.sics.ktoolbox.util.tracking.load.util;

import java.util.LinkedList;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TimeoutCounterRollingWindow implements TimeoutCounter {

    public static final int HISTORY_SIZE = 100;
    public static final int WINDOW_SIZE = 100;
    private double acceptableTimeoutsPercentage = 0.0001; // between 0 and 1

    private final LinkedList<Pair<Integer, Integer>> history = new LinkedList<>();
    private int currentSuccess = 0;
    private int currentTimeouts = 0;
    private int totalSuccess = 0;
    private int totalTimeouts = 0;

    @Override
    public void success() {
        currentSuccess++;
        if (currentSuccess + currentTimeouts == WINDOW_SIZE) {
            updateHistory();
        }
    }
    
    @Override
    public boolean timeout() {
        currentTimeouts++;
        if (currentSuccess + currentTimeouts == WINDOW_SIZE) {
            updateHistory();
        }
        return triggerTimeout();
    }
    
    @Override
    public void setAcceptableLoss(double acceptableLoss) {
        acceptableTimeoutsPercentage = acceptableLoss;
    }
    
    @Override
    public double getAcceptableLoss() {
        return acceptableTimeoutsPercentage;
    }
    
    private boolean triggerTimeout() {
        double percentage = (double)totalTimeouts/(totalTimeouts + totalSuccess);
        if(percentage < acceptableTimeoutsPercentage) {
            return false;
        }
        return true;
    }

    private void updateHistory() {
        if (history.size() == HISTORY_SIZE) {
            Pair<Integer, Integer> first = history.removeFirst();
            totalSuccess -= first.getValue0();
            totalTimeouts -= first.getValue1();
        }
        history.add(Pair.with(currentSuccess, currentTimeouts));
        totalSuccess += currentSuccess;
        totalTimeouts += currentTimeouts;
        currentSuccess = 0;
        currentTimeouts = 0;
    }
}
