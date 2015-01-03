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
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package se.sics.p2ptoolbox.croupier.core;

import java.util.UUID;
import se.sics.p2ptoolbox.croupier.api.CroupierSelectionPolicy;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.serialization.msg.HeaderField;
import se.sics.p2ptoolbox.serialization.msg.OverlayHeaderField;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierConfig {
    public final int viewSize;
    public final long shufflePeriod;
    public final int shuffleLength;
    public final CroupierSelectionPolicy policy;
    
    public CroupierConfig(int viewSize, long shufflePeriod, int shuffleLength, CroupierSelectionPolicy policy) {
        this.viewSize = viewSize;
        this.shufflePeriod = shufflePeriod;
        this.shuffleLength = shuffleLength;
        this.policy = policy;
    }

    public static enum MsgAliases {

        CROUPIER_NET_REQUEST(DirectMsgNetty.Request.class), CROUPIER_NET_RESPONSE(DirectMsgNetty.Response.class);
        public final Class aliasedClass;

        MsgAliases(Class aliasedClass) {
            this.aliasedClass = aliasedClass;
        }
    }

    public static enum OtherAliases {

        HEADER_FIELD(HeaderField.class), PEER_VIEW(PeerView.class);

        public final Class aliasedClass;

        OtherAliases(Class aliasedClass) {
            this.aliasedClass = aliasedClass;
        }
    }
    
    public static enum OtherSerializers {
        UUID(UUID.class), VOD_ADDRESS(VodAddress.class), OVERLAY_HEADER_FIELD(OverlayHeaderField.class);
        
        public final Class serializedClass;

        OtherSerializers(Class serializedClass) {
            this.serializedClass = serializedClass;
        }
    }
}
