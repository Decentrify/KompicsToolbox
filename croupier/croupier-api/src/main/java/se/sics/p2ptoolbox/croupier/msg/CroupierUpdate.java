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

package se.sics.p2ptoolbox.croupier.msg;

import com.google.common.base.Optional;
import se.sics.p2ptoolbox.util.update.SelfViewUpdate;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierUpdate<CV extends Object> implements SelfViewUpdate {
    public final boolean observer;
    public final Optional<CV> selfView;
    
    public CroupierUpdate(boolean observer, Optional<CV> selfView) {
        this.observer = observer;
        this.selfView = selfView;
    }

    @Override
    public String toString() {
        return "CROUPIER_VIEW_UPDATE";
    }
    
    public static CroupierUpdate observer() {
        return new CroupierUpdate(true, Optional.absent());
    }
    
    public static <CV extends Object> CroupierUpdate update(CV updateView) {
        return new CroupierUpdate(false, Optional.of(updateView));
    }
}
