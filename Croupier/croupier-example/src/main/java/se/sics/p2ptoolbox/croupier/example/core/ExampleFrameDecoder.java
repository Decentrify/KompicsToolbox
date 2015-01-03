/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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

package se.sics.p2ptoolbox.croupier.example.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.BaseMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.p2ptoolbox.croupier.core.CroupierConfig;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.SerializationContextImpl;
import se.sics.p2ptoolbox.serialization.serializer.SerializerAdapter;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ExampleFrameDecoder extends BaseMsgFrameDecoder {

    public static final byte ONEWAY = (byte)0x90;
    public static final byte REQUEST = (byte)0x91;
    public static final byte RESPONSE = (byte)0x92;

    private static SerializationContext context = new SerializationContextImpl();

    public static void reset() {
        context = new SerializationContextImpl();
    }
    
    public static SerializationContext getContext() {
        return context;
    }
    
    public static void register() {
        try {
            context.registerAlias(CroupierConfig.MsgAliases.CROUPIER_NET_REQUEST.aliasedClass, CroupierConfig.MsgAliases.CROUPIER_NET_REQUEST.toString(), REQUEST);
            context.registerAlias(CroupierConfig.MsgAliases.CROUPIER_NET_RESPONSE.aliasedClass, CroupierConfig.MsgAliases.CROUPIER_NET_RESPONSE.toString(), RESPONSE);
        } catch (SerializationContext.DuplicateException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ExampleFrameDecoder() {
        super();
    }

    @Override
    protected RewriteableMsg decodeMsg(ChannelHandlerContext ctx, ByteBuf buffer) throws MessageDecodingException {
        // See if msg is part of parent project, if yes then return it.
        // Otherwise decode the msg here.
        RewriteableMsg msg = super.decodeMsg(ctx, buffer);
        if (msg != null) {
            return msg;
        }

        switch (opKod) {
            case ONEWAY:
                SerializerAdapter.OneWay oneWayS = new SerializerAdapter.OneWay();
                return oneWayS.decodeMsg(buffer);
            case REQUEST:
                SerializerAdapter.Request requestS = new SerializerAdapter.Request();
                return requestS.decodeMsg(buffer);
            case RESPONSE:
                SerializerAdapter.Response responseS = new SerializerAdapter.Response();
                return responseS.decodeMsg(buffer);
            default:
                return null;
        }
    }
}
