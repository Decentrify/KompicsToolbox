/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.croupier.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ProbabilisticHelper {
    public static <C extends Object> List<C> generateRandomSample(Random rand, List<C> values, int sampleSize) {
        List<C> result = new ArrayList<>();
        if (sampleSize >= values.size()) {
            //return a copy of all entries
            result.addAll(values);
            return result;
        }
        // Don Knuth, The Art of Computer Programming, Algorithm S(3.4.2)
        int t = 0, m = 0, n = sampleSize;
        while (m < sampleSize) {
            int x = rand.nextInt(n - t);
            if (x < sampleSize - m) {
                result.add(values.get(t));
                m += 1;
                t += 1;
            } else {
                t += 1;
            }
        }
        return result;
    }
    
    //TODO Alex check if it matched to Abhi's soft max and replace
    public static int softMaxIndex(Random rand, int listSize, double temperature) {
        double rnd = rand.nextDouble();
        double total = 0.0d;
        double[] values = new double[listSize];
        int j = listSize + 1;
        for (int i = 0; i < listSize; i++) {
            // get inverse of values - lowest have highest value.
            double val = j;
            j--;
            values[i] = Math.exp(val / temperature);
            total += values[i];
        }

        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                values[i] += values[i - 1];
            }
            // normalise the probability
            double normalisedReward = values[i] / total;
            if (normalisedReward >= rnd) {
                return i;
            }
        }
        return -1;
    }
}
