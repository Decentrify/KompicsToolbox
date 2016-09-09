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

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExpMovingAvg_Broken {

    //**************************************************************************
    private final double alpha = 0.125;
    private final double beta = 0.25;
    private final long K = 4;
    //**************************************************************************
    private final double min;
    private double avg;
    private double var;
    private double val;

    public ExpMovingAvg_Broken(double first, double min) {
        this.min = min;
        avg = 0.0;
        var = 0.0;
        val = -1.0;
    }

    public void update(double instVal) {
        if (val == -1) {
            // SRTT <- R, RTTVAR <- R/2, RTO <- SRTT + max (G, KRTTVAR)
            this.avg = instVal;
            this.var = instVal / 2.0;
            this.val = avg + (K * var);
        } else {
            // RTTVAR <- (1 - beta) * RTTVAR + beta * |SRTT - R'|
            this.var = (1 - beta) * var + beta * Math.abs((avg - instVal));
            // SRTT <- (1 - alpha) * SRTT + alpha * R'
            this.avg = (1 - alpha) * avg + alpha * instVal;
            // RTO = AVG + K x VAR;
            this.val = avg + K * var;
        }
    }
    
    public double get() {
        if (this.val < min) {
            return min;
        } else {
            return val;
        }
    }
}
