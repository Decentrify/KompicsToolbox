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
package se.sics.ktoolbox.util.predict;

import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SimpleSmoothing implements SmoothingHeuristic {
//    protected final double[] adjustments = new double[]{-1, -0.7, -0.4, -0.1, 0.1, 0.4, 0.7, 1};
    protected final double[][] adjustments;
    {
        adjustments = new double[4][];
        //0-25% - we want to be more aggresive
        adjustments[0] = new double[]{-0.4, -0.2, 0, 0.3, 0.6, 0.8, 0.9, 1};
        //25-50% - be conservative we do not want to reach max buffer too fast - as that will slow us afterwards, making our effort here pointless
        adjustments[1] = new double[]{-0.8, -0.6, -0.4, 0, 0.2, 0.4, 0.7, 1};
        //50-75% - we might be going to our platteau too fast, lets be on the slowing side a bit
        adjustments[2] = new double[]{-1, -0.7, -0.4, -0.2, 0, 0.4, 0.6, 0.8};
        //75%-100% - we will fill up the buffers
        adjustments[3] = new double[]{-1, -0.9, -0.8, -0.6, -0.3, 0, 0.2, 0.4};
    }
    protected static enum Values {
        U_D65(1.73), //rough approximation of tan 60 deg
        U_D45(1), //rough approximation of tan 45 deg
        U_D30(0.58), //rough approximation of tan 30 deg
        H(0), //horizontal line
        D_D30(-0.58), //rough approximation of tan 330 deg
        D_D45(-1), //rough approximation of tan 315 deg
        D_D60(-1.73); //rough approximation of tan 300 deg
    
        public double val;
        
        Values(double val) {
            this.val = val;
        }
    }   
    
    @Override
    public double getAdjustment(double current, double previous, Pair<Double, Double> threshold) {
        double usage = current / (threshold.getValue1() - threshold.getValue0());
        double[] adjustmentRate;
        if(usage < 0.25) {
            adjustmentRate = adjustments[0];
        } else if(usage < 0.5) {
            adjustmentRate = adjustments[1];
        } else if(usage < 0.75) {
            adjustmentRate = adjustments[2];
        } else {
            adjustmentRate = adjustments[3];
        }
        double val = current - previous;
        for(Values v : Values.values()) {
            if(val > v.val) {
                return adjustmentRate[v.ordinal()];
            }
        }
        return adjustmentRate[adjustments.length-1];
    }
}
