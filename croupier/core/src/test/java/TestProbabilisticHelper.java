
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import se.sics.ktoolbox.croupier.util.ProbabilisticHelper;

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

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestProbabilisticHelper {
    @Test
    public void test1() {
        List<Integer> values = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            values.add(i);
        }

        for(int i = 0; i<50; i++) {
            System.out.println(values);
            System.out.println(ProbabilisticHelper.generateRandomSample(new Random(i), values, 10));
        }
    }
}
