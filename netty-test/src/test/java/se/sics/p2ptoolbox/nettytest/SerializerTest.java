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

package se.sics.p2ptoolbox.nettytest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import junit.framework.Assert;
import org.junit.Test;
import se.sics.p2ptoolbox.nettytest.msg.MsgA;
import se.sics.p2ptoolbox.serialization.api.BaseCategories;
import se.sics.p2ptoolbox.serialization.api.SerializationContext;
import se.sics.p2ptoolbox.serialization.api.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializerTest {
    @Test
    public void testMsgARequest() throws Serializer.SerializerException {
        SystemFrameDecoder m = new SystemFrameDecoder();
        SerializationContext context = m.getContext();
        
        MsgA.Request expectedReq = new MsgA.Request(UUID.randomUUID(), 1);
        ByteBuf writeBuf = Unpooled.buffer();
        context.getSerializer(MsgA.Request.class).encode(context, writeBuf, expectedReq);
        
        ByteBuf readBuf = Unpooled.wrappedBuffer(writeBuf.array());
        byte opcode = readBuf.readByte();
        Assert.assertEquals(Byte.valueOf(opcode), context.getOpcode(BaseCategories.PAYLOAD, MsgA.Request.class));
        
        MsgA.Request deserializedReq = context.getSerializer(MsgA.Request.class).decode(context, readBuf);
        Assert.assertEquals(expectedReq, deserializedReq);
    }

    private void AssertEquals(Byte valueOf, Byte opcode) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
