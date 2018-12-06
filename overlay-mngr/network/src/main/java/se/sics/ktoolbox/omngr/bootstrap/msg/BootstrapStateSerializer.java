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
package se.sics.ktoolbox.omngr.bootstrap.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.LinkedList;
import java.util.List;
import se.sics.kompics.util.Identifier;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapState;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapStateSerializer {

  public static class Init implements Serializer {

    private final int id;

    public Init(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
    }

    @Override
    public BootstrapState.Init fromBinary(ByteBuf buf, Optional<Object> hint) {
      return new BootstrapState.Init();
    }
  }

  public static class Sample implements Serializer {

    private final int id;

    public Sample(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      BootstrapState.Sample obj = (BootstrapState.Sample) o;
      buf.writeInt(obj.sample.size());
      obj.sample.forEach((val) -> Serializers.toBinary(val, buf));
    }

    @Override
    public BootstrapState.Sample fromBinary(ByteBuf buf, Optional<Object> hint) {
      int sampleSize = buf.readInt();
      List<KAddress> sample = new LinkedList<>();
      for (int i = 0; i < sampleSize; i++) {
        sample.add((KAddress)Serializers.fromBinary(buf, hint));
      }
      return new BootstrapState.Sample(sample);
    }
  }
}
