
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.overlaymngr.OMngrSerializerSetup;
import se.sics.ktoolbox.overlaymngr.util.ServiceView;
import se.sics.p2ptoolbox.croupier.CroupierSerializerSetup;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

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

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SerializerTest {
    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = CroupierSerializerSetup.registerSerializers(serializerId);
        serializerId = OMngrSerializerSetup.registerSerializers(serializerId);
    }
    @Test
    public void testServiceView() {
        Object sv = new ServiceView(new ArrayList<ByteBuffer>());
        ByteBuf buf = Unpooled.buffer();
        Serializers.toBinary(sv, buf);
    }
}
