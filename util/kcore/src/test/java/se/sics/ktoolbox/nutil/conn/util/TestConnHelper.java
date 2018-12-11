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
package se.sics.ktoolbox.nutil.conn.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import se.sics.ktoolbox.nutil.conn.ConnCtrl;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnState;
import se.sics.ktoolbox.nutil.conn.ConnStatus;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestConnHelper {

  public static class AutoCloseClientCtrl<S extends ConnState, P extends ConnState> implements ConnCtrl<S, P> {

    private final HashMap<ConnIds.ConnId, Integer> connected = new HashMap<>();
    private final int heartbeatCounts;

    public AutoCloseClientCtrl(int heartbeatCounts) {
      this.heartbeatCounts = heartbeatCounts;
    }

    @Override
    public Map<ConnIds.ConnId, ConnStatus> selfUpdate(ConnIds.InstanceId instanceId, S state) {
      Map<ConnIds.ConnId, ConnStatus> updateAll = connected.keySet().stream()
        .collect(Collectors.toMap((connId) -> connId, (connId) -> ConnStatus.Base.CLIENT_STATE));
      return updateAll;
    }

    @Override
    public ConnStatus partnerUpdate(ConnIds.ConnId connId, S selfState,
      ConnStatus peerStatus, KAddress peer, P peerState) {
      if (peerStatus.equals(ConnStatus.Base.CONNECTED)) {
        connected.put(connId, heartbeatCounts);
        return ConnStatus.Base.CONNECTED_ACK;
      } else if (ConnStatus.Base.DISCONNECTED.equals(peerStatus)
        || ConnStatus.Base.SERVER_STATE.equals(peerStatus)) {
        return ConnStatus.Base.NOTHING;
      } else if (ConnStatus.Base.HEARTBEAT_ACK.equals(peerStatus)) {
        if (connected.containsKey(connId)) {
          int heartbeatsLeft = connected.get(connId) - 1;
          if (heartbeatsLeft == 0) {
            return ConnStatus.Base.DISCONNECT;
          } else {
            connected.put(connId, heartbeatsLeft);
            return ConnStatus.Base.NOTHING;
          }
        } else {
          return ConnStatus.Base.DISCONNECT;
        }
      } else {
        throw new RuntimeException("ups");
      }
    }

    @Override
    public void close(ConnIds.ConnId connId) {
      connected.remove(connId);
    }
  }
}
