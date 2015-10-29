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
package se.sics.ktoolbox.overlaymngr;

import java.util.HashSet;
import java.util.Set;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption;
import se.sics.p2ptoolbox.util.config.options.OpenAddressBootstrapOption;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayMngrConfig implements KConfigLevel {

    public static final byte[] GLOBAL_GROUPIER_ID = new byte[]{};

    public final static OpenAddressBootstrapOption bootstrap = new OpenAddressBootstrapOption("globalcroupier.bootstrap", new OverlayMngrConfig());
    
    public static Integer getGlobalCroupierIntegerId() {
        return 0;
    }
    
    public static boolean isGlobalCroupier(byte[] id) {
        return (id.equals(new byte[]{}) || id.equals(new byte[]{0,0,0,0}));
    }

    @Override
    public Set<String> canWrite() {
        Set<String> canWrite = new HashSet<>();
        canWrite.add(toString());
        return canWrite;
    }

    @Override
    public String toString() {
        return "OverlayMngrConfig";
    }
}
