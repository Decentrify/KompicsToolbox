package se.sics.ktoolbox.util.predict;

import org.javatuples.Pair;

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
/**
 * STGMAvg - smooth thresholded growing moving average
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class STGMAvg {

    private final ExpMovingAvg avg;
    private final Pair<Double, Double> threshold;
    private final SmoothingHeuristic smoothing;
    private double previousAvg;

    public STGMAvg(ExpMovingAvg avg, Pair<Double, Double> threshold, SmoothingHeuristic smoothing) {
        this.avg = avg;
        this.threshold = threshold;
        this.previousAvg = threshold.getValue0();
        this.smoothing = smoothing;
    }

    /**
     * @param instVal
     * @return adjustment as a value between [-1,1]
     */
    public double update(double instVal) {
        double currentAvg = avg.get();
        double adjustment = smoothing.getAdjustment(currentAvg, previousAvg, threshold);
        avg.update(instVal);
        previousAvg = currentAvg;
        return adjustment;
    }

    public double get() {
        return avg.get();
    }
}
