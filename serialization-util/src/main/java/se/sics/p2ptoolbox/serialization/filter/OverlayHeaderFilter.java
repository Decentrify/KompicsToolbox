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
package se.sics.p2ptoolbox.serialization.filter;

import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.kompics.ChannelFilter;
import se.sics.p2ptoolbox.serialization.msg.NetContentMsg;
import se.sics.p2ptoolbox.serialization.msg.NetMsg;
import se.sics.p2ptoolbox.serialization.msg.OverlayHeaderField;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlayHeaderFilter extends ChannelFilter<DirectMsg, Integer> {

    public OverlayHeaderFilter(Integer overlay) {
        super(DirectMsg.class, overlay, true);
    }

    @Override
    public Integer getValue(DirectMsg msg) {
        if (msg instanceof NetMsg.Request) {
            NetMsg.Request req = (NetMsg.Request) msg;
            OverlayHeaderField ohf = (OverlayHeaderField) req.header.get("overlay");
            if (ohf == null) {
                return -1;
            }
            return ohf.overlayId;
        } else if (msg instanceof NetMsg.Response) {
            NetMsg.Response resp = (NetMsg.Response) msg;
            OverlayHeaderField ohf = (OverlayHeaderField) resp.header.get("overlay");
            if (ohf == null) {
                return -1;
            }
            return ohf.overlayId;
        } else if (msg instanceof NetMsg.OneWay) {
            NetMsg.OneWay oneWay = (NetMsg.OneWay) msg;
            OverlayHeaderField ohf = (OverlayHeaderField) oneWay.header.get("overlay");
            if (ohf == null) {
                return -1;
            }
            return ohf.overlayId;
        } else {
            return -1;
        }
    }
}
