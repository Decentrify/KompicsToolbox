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

import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnIds {

  public static class InstanceId implements Identifier {

    public final Identifier nodeId;
    public final Identifier instanceId;

    public InstanceId(Identifier nodeId, Identifier instanceId) {
      this.nodeId = nodeId;
      this.instanceId = instanceId;
    }

    @Override
    public int partition(int nrPartitions) {
      int part1 = nodeId.partition(nrPartitions);
      int part2 = instanceId.partition(nrPartitions);
      if (Integer.MAX_VALUE - part1 < part2) {
        return part2 - (Integer.MAX_VALUE - part1);
      } else {
        return part1 + part2;
      }
    }

    @Override
    public int compareTo(Identifier o) {
      int res;
      InstanceId that = (InstanceId) o;
      res = this.nodeId.compareTo(that.nodeId);
      if (res == 0) {
        res = this.instanceId.compareTo(that.instanceId);
      }
      return res;
    }
  }

  public static class ConnId implements Identifier {

    public final InstanceId serverId;
    public final InstanceId clientId;

    public ConnId(InstanceId serverId, InstanceId clientId) {
      this.serverId = serverId;
      this.clientId = clientId;
    }

    @Override
    public int partition(int nrPartitions) {
      int part1 = serverId.partition(nrPartitions);
      int part2 = clientId.partition(nrPartitions);
      if (Integer.MAX_VALUE - part1 < part2) {
        return part2 - (Integer.MAX_VALUE - part1);
      } else {
        return part1 + part2;
      }
    }

    @Override
    public int compareTo(Identifier o) {
      int res;
      InstanceId that = (InstanceId) o;
      res = this.serverId.compareTo(that.nodeId);
      if (res == 0) {
        res = this.clientId.compareTo(that.instanceId);
      }
      return res;
    }
  }
}
