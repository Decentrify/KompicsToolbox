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
package se.sics.ktoolbox.nutil.conn;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.javatuples.Pair;
import se.sics.ktoolbox.nutil.conn.ConnIds.ConnId;
import se.sics.ktoolbox.nutil.conn.ConnIds.InstanceId;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnHelper {

  public static <C extends ConnState> ServerListener<C> noServerListener() {
    return new ServerListener<C>() {
      @Override
      public Pair<ConnStatus, Optional<Connection.Server>> connect(ConnId connId, ConnStatus peerStatus,
        KAddress peer, Optional<C> peerState) {
        return Pair.with(ConnStatus.Base.DISCONNECTED, Optional.empty());
      }
    };
  }
  public static class SimpleConnCtrl<S extends ConnState, P extends ConnState> implements ConnCtrl<S,P> {

    @Override
    public Map<ConnId, ConnStatus> selfUpdate(InstanceId instanceId, S state) {
      return new HashMap<>();
    }

    @Override
    public Pair<ConnId, ConnStatus> partnerUpdate(ConnId connId, S selfState,
      ConnStatus peerStatus, KAddress peer, Optional<P> peerState) {
      if (peerStatus.equals(ConnStatus.Base.CONNECT)) {
        return Pair.with(connId, ConnStatus.Base.CONNECTED);
      } else if(peerStatus.equals(ConnStatus.Base.CONNECTED)) {
        return Pair.with(connId, ConnStatus.Base.CONNECTED);
      } else if (peerStatus.equals(ConnStatus.Base.DISCONNECT)) {
        return Pair.with(connId, ConnStatus.Base.DISCONNECTED);
      } else if (peerStatus.equals(ConnStatus.Base.HEARTBEAT)) {
        return Pair.with(connId, ConnStatus.Base.HEARTBEAT_ACK);
      } else if (peerStatus.equals(ConnStatus.Base.HEARTBEAT_ACK)) {
        return Pair.with(connId, ConnStatus.Base.HEARTBEAT_ACK);
      }
      throw new RuntimeException("ups");
    }

    @Override
    public void close(ConnId connId) {
    }
  }
}
