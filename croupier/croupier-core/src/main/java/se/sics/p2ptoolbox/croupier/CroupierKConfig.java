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
package se.sics.p2ptoolbox.croupier;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Set;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption;
import se.sics.p2ptoolbox.util.config.KConfigOption.Basic;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierKConfig implements KConfigLevel {
    private final static Basic<String> policy = new Basic("croupier.policy", String.class, new CroupierKConfig());
    public final static CSelectionPolicyOption sPolicy = new CSelectionPolicyOption("croupier.sPolicy", new CroupierKConfig());
    public final static Basic<Integer> viewSize = new Basic("croupier.viewSize", Integer.class, new CroupierKConfig());
    public final static Basic<Integer> shuffleSize = new Basic("croupier.shuffleSize", Integer.class, new CroupierKConfig());
    public final static Basic<Long> shufflePeriod = new Basic("croupier.shufflePeriod", Long.class, new CroupierKConfig());
    public final static Basic<Long> shuffleTimeout = new Basic("croupier.shuffleTimeout", Long.class, new CroupierKConfig());
    public final static Basic<Double> softMaxTemp = new Basic("croupier.softMaxTemperature", Double.class, new CroupierKConfig());

    @Override
    public Set<String> canWrite() {
        Set<String> canWrite = new HashSet<>();
        canWrite.add(toString());
        return canWrite;
    }

    @Override
    public String toString() {
        return "NetworkMngrConfig";
    }

    public static class CSelectionPolicyOption extends KConfigOption.Composite<CroupierSelectionPolicy> {

        public CSelectionPolicyOption(String name, KConfigLevel lvl) {
            super(name, CroupierSelectionPolicy.class, lvl);
        }

        @Override
        public Optional<CroupierSelectionPolicy> read(KConfigCache config) {
            Optional<String> sPolicy = config.read(policy);
            if (!sPolicy.isPresent()) {
                return Optional.absent();
            }
            CroupierSelectionPolicy parsedPolicy = CroupierSelectionPolicy.create(sPolicy.get());
            return Optional.fromNullable(parsedPolicy);
        }
    }
}
