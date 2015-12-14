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

package se.sics.ktoolbox.chunkmanager.util;

import se.sics.kompics.ChannelSelector;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CMInternalFilter extends ChannelSelector<KContentMsg, Boolean> {

    public CMInternalFilter() {
        super(KContentMsg.class, true, true);
    }
    
    @Override
    public Boolean getValue(KContentMsg msg) {
        if (!msg.getHeader().getProtocol().equals(Transport.UDP)) {
            return false;
        }
        if(!(msg.getContent() instanceof Chunk)) {
            return false;
        }
        return true;
    }
}
