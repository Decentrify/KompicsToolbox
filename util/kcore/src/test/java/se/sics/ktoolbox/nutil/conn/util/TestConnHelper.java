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
import java.util.Optional;
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
    private final int updateCount;

    public AutoCloseClientCtrl(int updateCount) {
      this.updateCount = updateCount;
    }

    @Override
    public ConnStatus.Decision connect(ConnIds.ConnId connId, KAddress partnerAdr, S selfState, Optional<P> partnerState) {
      return ConnStatus.Decision.PROCEED;
    }

    @Override
    public ConnStatus.Decision connected(ConnIds.ConnId connId, S selfState, P partnerState) {
      connected.put(connId, updateCount);
      return ConnStatus.Decision.PROCEED;
    }

    @Override
    public ConnStatus.Decision selfUpdate(ConnIds.ConnId connId, S selfState, P partnerState) {
      if (connected.containsKey(connId)) {
        int updatesLeft = connected.get(connId) - 1;
        if (updatesLeft == 0) {
          return ConnStatus.Decision.DISCONNECT;
        } else {
          connected.put(connId, updatesLeft);
          return ConnStatus.Decision.PROCEED;
        }
      } else {
        return ConnStatus.Decision.DISCONNECT;
      }
    }

    @Override
    public ConnStatus.Decision serverUpdate(ConnIds.ConnId connId, S selfState, P partnerState) {
      return ConnStatus.Decision.PROCEED;
    }

    @Override
    public void close(ConnIds.ConnId connId) {
      connected.remove(connId);
    }
  }
}
