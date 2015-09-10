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
package se.sics.p2ptoolbox.util.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ChannelFilter;
import se.sics.kompics.network.Msg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PortTrafficFilter extends ChannelFilter<Msg, Integer> {
    private static final Logger LOG = LoggerFactory.getLogger("Test");
    private String logPrefix = "";

    private final int srcId;

    public PortTrafficFilter(Integer port, Integer srcId) {
        super(Msg.class, port, true);
        this.srcId = srcId;
    }

    @Override
    public Integer getValue(Msg msg) {
        BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Object> contentMsg = (BasicContentMsg) msg;
        if (contentMsg.getSource().getId().equals(srcId)) {
            LOG.error("src" + contentMsg.getSource().getPort());
            return contentMsg.getSource().getPort();
        }
        if (contentMsg.getDestination().getId().equals(srcId)) {
            LOG.error("dst" + contentMsg.getDestination().getPort());
            return contentMsg.getDestination().getPort();
        }
        LOG.error("none" + srcId + " " +  contentMsg.getSource() + " " + contentMsg.getDestination());
        return null;
    }
}
