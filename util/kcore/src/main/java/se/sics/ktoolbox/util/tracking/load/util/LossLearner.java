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

import org.javatuples.Triplet;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LossLearner {

    private Triplet<Long, Double, Integer> current = null;
    private Triplet<Long, Double, Integer> previous = null;
    private Triplet<Long, Double, Integer> left = null;
    private Triplet<Long, Double, Integer> right = null;

    public Double next(long bandwidth, double acceptableLoss) {
        if (current == null) {
            if (previous == null) {
                current = Triplet.with(bandwidth, acceptableLoss, 2);
            } else {
                current = previous.setAt0(bandwidth);
            }
            return current.getValue1() - current.getValue1() / current.getValue2();
        }
        if (acceptableLoss > current.getValue1()) {
            right = Triplet.with(bandwidth, acceptableLoss, current.getValue2());
        } else {
            left = Triplet.with(bandwidth, acceptableLoss, current.getValue2());
            if (right == null) {
                return current.getValue1() + current.getValue1() / current.getValue2();
            }
        }
        if (left == null || right == null) {
            throw new RuntimeException("should not happen - first explore left, then right");
        }
        long currentBandwidth = (long) (current.getValue0() * (1 - current.getValue1()));
        long leftBandwidth = (long) (left.getValue0() * (1 - left.getValue1()));
        long rightBandwidth = (long) (right.getValue0() * (1 - right.getValue1()));

        if (0.8 * currentBandwidth <= leftBandwidth) {
            //more throughput with less loss - we want this
            current = current.setAt2(decreaseFactor(current.getValue2()));
            right = null;
            left = null;
            return current.getValue1() - current.getValue1() / current.getValue2();
        }
        if (1.2 * currentBandwidth < rightBandwidth) {
            //more throughput with more loss - acceptable
            current = current.setAt2(decreaseFactor(current.getValue2()));
            left = null;
            right = null;
            return current.getValue1() - current.getValue1() / current.getValue2();
        }
        previous = current.setAt2(increaseFactor(current.getValue2()));
        current = null;
        right = null;
        left = null;
        return previous.getValue1();
    }

    private int increaseFactor(int factor) {
        if (factor == 16) {
            return factor;
        }
        return factor * 2;
    }

    private int decreaseFactor(int factor) {
        if (factor == 2) {
            return factor;
        }
        return factor / 2;
    }
}
