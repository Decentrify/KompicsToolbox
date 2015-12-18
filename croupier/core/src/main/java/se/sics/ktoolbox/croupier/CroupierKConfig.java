/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNUs General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.ktoolbox.croupier;

import com.google.common.base.Optional;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.KConfigOption;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierKConfig {

    public final static CSelectionPolicyOption selectionPolicy = new CSelectionPolicyOption("croupier.selectionPolicy");
    public final static KConfigOption.Basic<Integer> viewSize = new KConfigOption.Basic("croupier.viewSize", Integer.class);
    public final static KConfigOption.Basic<Integer> shuffleSize = new KConfigOption.Basic("croupier.shuffleSize", Integer.class);
    public final static KConfigOption.Basic<Long> shufflePeriod = new KConfigOption.Basic("croupier.shufflePeriod", Long.class);
    public final static KConfigOption.Basic<Integer> shuffleSpeedFactor = new KConfigOption.Basic("croupier.shuffleSpeedFactor", Integer.class);
    public final static KConfigOption.Basic<Long> shuffleTimeout = new KConfigOption.Basic("croupier.shuffleTimeout", Long.class);
    public final static KConfigOption.Basic<Boolean> softMax = new KConfigOption.Basic("croupier.softMax", Boolean.class);
    public final static KConfigOption.Basic<Double> softMaxTemp = new KConfigOption.Basic("croupier.softMaxTemperature", Double.class);

    public static class CSelectionPolicyOption extends KConfigOption.Base<CroupierSelectionPolicy> {

        public CSelectionPolicyOption(String name) {
            super(name, CroupierSelectionPolicy.class);
        }

        @Override
        public Optional<CroupierSelectionPolicy> readValue(Config config) {
            Optional sPolicy = config.readValue(name);
            if (sPolicy.isPresent()) {
                if (sPolicy.get() instanceof String) {
                    CroupierSelectionPolicy parsedPolicy = CroupierSelectionPolicy.create((String)sPolicy.get());
                    return Optional.fromNullable(parsedPolicy);
                } else if(sPolicy.get() instanceof CroupierSelectionPolicy) {
                    return sPolicy;
                }
            }
            return Optional.absent();
        }
    }
}
