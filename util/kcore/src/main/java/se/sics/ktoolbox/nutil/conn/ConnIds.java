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

import java.util.Objects;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.MathHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnIds {

  public static class InstanceId implements Identifier {

    public final Identifier overlayId;
    public final Identifier nodeId;
    public final Identifier batchId;
    public final Identifier instanceId;
    public final Boolean server;

    public InstanceId(Identifier overlayId, Identifier nodeId, Identifier batchId, Identifier instanceId,
      boolean server) {
      this.overlayId = overlayId;
      this.nodeId = nodeId;
      this.batchId = batchId;
      this.instanceId = instanceId;
      this.server = server;
    }

    @Override
    public int partition(int nrPartitions) {
      return MathHelper.moduloWrappedSum(nrPartitions,
        overlayId.partition(nrPartitions),
        nodeId.partition(nrPartitions),
        batchId.partition(nrPartitions),
        instanceId.partition(nrPartitions),
        server ? 0 : 1);
    }

    @Override
    public int compareTo(Identifier o) {
      int res;
      InstanceId that = (InstanceId) o;
      res = this.overlayId.compareTo(that.overlayId);
      if (res == 0) {
        res = this.nodeId.compareTo(that.nodeId);
        if (res == 0) {
          res = this.batchId.compareTo(that.batchId);
          if (res == 0) {
            res = this.instanceId.compareTo(that.instanceId);
            if (res == 0) {
              res = this.server.compareTo(that.server);
            }
          }
        }
      }
      return res;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 83 * hash + Objects.hashCode(this.overlayId);
      hash = 83 * hash + Objects.hashCode(this.nodeId);
      hash = 83 * hash + Objects.hashCode(this.batchId);
      hash = 83 * hash + Objects.hashCode(this.instanceId);
      hash = 83 * hash + Objects.hashCode(this.server);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final InstanceId other = (InstanceId) obj;
      if (!Objects.equals(this.overlayId, other.overlayId)) {
        return false;
      }
      if (!Objects.equals(this.nodeId, other.nodeId)) {
        return false;
      }
      if (!Objects.equals(this.batchId, other.batchId)) {
        return false;
      }
      if (!Objects.equals(this.instanceId, other.instanceId)) {
        return false;
      }
      if (!Objects.equals(this.server, other.server)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return '<' + "oid:" + overlayId + "nid:" + nodeId + ", bid:" + batchId + ", iid:" + instanceId + 
        ", s:" + server + '>';
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

    @Override
    public int hashCode() {
      int hash = 3;
      hash = 37 * hash + Objects.hashCode(this.serverId);
      hash = 37 * hash + Objects.hashCode(this.clientId);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ConnId other = (ConnId) obj;
      if (!Objects.equals(this.serverId, other.serverId)) {
        return false;
      }
      if (!Objects.equals(this.clientId, other.clientId)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return '<' + "sid:" + serverId + ", cid:" + clientId + '>';
    }
  }
}
