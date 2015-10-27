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
package se.sics.p2ptoolbox.gradient;

import java.util.HashSet;
import java.util.Set;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption.Basic;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class GradientKConfig implements KConfigLevel {
    public final static Basic<Integer> viewSize = new Basic("gradient.viewSize", Integer.class, new GradientKConfig());
    public final static Basic<Integer> shuffleSize = new Basic("gradient.shuffleSize", Integer.class, new GradientKConfig());
    public final static Basic<Long> shufflePeriod = new Basic("gradient.shufflePeriod", Long.class, new GradientKConfig());
    public final static Basic<Long> shuffleTimeout = new Basic("gradient.shuffleTimeout", Long.class, new GradientKConfig());
    public final static Basic<Double> softMaxTemp = new Basic("gradient.softMaxTemperature", Double.class, new GradientKConfig());
    public final static Basic<Integer> oldThreshold = new Basic("gradient.oldThreshold", Integer.class, new GradientKConfig());
    
    @Override
    public Set<String> canWrite() {
        Set<String> canWrite = new HashSet<>();
        canWrite.add(toString());
        return canWrite;
    }

    @Override
    public String toString() {
        return "GradientKConfig";
    }
}
