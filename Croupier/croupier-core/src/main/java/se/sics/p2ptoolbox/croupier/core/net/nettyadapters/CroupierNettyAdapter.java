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

package se.sics.p2ptoolbox.croupier.core.net.nettyadapters;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.croupier.api.CroupierMsg;
import se.sics.p2ptoolbox.croupier.core.net.CroupierSerializer;
import se.sics.p2ptoolbox.croupier.core.net.CroupierContext;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierNettyAdapter {

    public static class Request extends DirectMsgNettyFactory.Request implements NetworkNettyAdapter {
        
        //**********NettyAdapter
        @Override
        public RewriteableMsg decodeMsg(ByteBuf buffer) throws NetworkNettyAdapter.DecodingException {
            try {
                return decode(buffer);
            } catch (MessageDecodingException ex) {
                throw new NetworkNettyAdapter.DecodingException(ex);
            }
        }

        @Override
        public ByteBuf encodeMsg(ByteBuf buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        //**********DirectMsgNettyFactory.Request
        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            byte opCode = buffer.readByte();
            int overlayId = buffer.readInt();
            CroupierSerializer currentAdapter = CroupierContext.getAdapter(opCode);
            CroupierMsg.Request payload = (CroupierMsg.Request) currentAdapter.decode(CroupierContext.getContext(buffer);
            return new GvodNetMsg.Request(vodSrc, vodDest, payload);
        }
    }

    public static class Response extends DirectMsgNettyFactory.Response implements NetworkNettyAdapter {

        //**********NettyAdapter
        @Override
        public RewriteableMsg decodeMsg(ByteBuf buffer) throws NetworkNettyAdapter.DecodingException {
            try {
                return decode(buffer);
            } catch (MessageDecodingException ex) {
                throw new NetworkNettyAdapter.DecodingException(ex);
            }
        }

        @Override
        public ByteBuf encodeMsg(ByteBuf buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        //**********DirectMsgNettyFactory.Request
        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            byte opCode = buffer.readByte();
            LocalNettyAdapter<? extends KompicsEvent> currentAdapter = GVoDAdapterFactory.getAdapter(opCode);
            GvodMsg.Response payload = (GvodMsg.Response) currentAdapter.decode(buffer);
            return new GvodNetMsg.Response(vodSrc, vodDest, payload);
        }
    }
}
