///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.p2ptoolbox.nettytest;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import java.util.UUID;
//import se.sics.gvod.common.msgs.MessageDecodingException;
//import se.sics.gvod.net.BaseMsgFrameDecoder;
//import se.sics.gvod.net.msgs.RewriteableMsg;
//import se.sics.p2ptoolbox.nettytest.msg.MsgA;
//import se.sics.p2ptoolbox.nettytest.msg.MsgB;
//import se.sics.p2ptoolbox.nettytest.msgserializer.MsgASerializer;
//import se.sics.p2ptoolbox.nettytest.msgserializer.MsgBSerializer;
//import se.sics.p2ptoolbox.nettytest.msgserializer.UUIDSerializer;
//import se.sics.p2ptoolbox.serialization.MyNettyMsg;
//import se.sics.p2ptoolbox.serialization.MyNettySerializer;
//import se.sics.p2ptoolbox.serialization.SerializationContextImpl;
//import se.sics.p2ptoolbox.serialization.api.BaseCategories;
//import se.sics.p2ptoolbox.serialization.api.SerializationContext;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class SystemFrameDecoder extends BaseMsgFrameDecoder {
//
//    private static final byte MAX = (byte) 0x255;
//
//    //netty
//    public static final byte TEST_NET_REQUEST = 0x60;
//    public static final byte TEST_NET_RESPONSE = 0x61;
//    public static final byte TEST_NET_ONEWAY = 0x62;
//
//    private final SerializationContext context;
//
//    public SystemFrameDecoder() {
//        super();
//        try {
//            this.context = buildContext();
//
//            MyNettySerializer.setSerializationContext(context);
//            MyNettyMsg.setContext(context);
//        } catch (SerializationContext.DuplicateException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    private SerializationContext buildContext() throws SerializationContext.DuplicateException {
//        //context serializers
//        SerializationContext ctxt = new SerializationContextImpl();
//
//        //category registration
//        ctxt.registerCategory(BaseCategories.BULK, (byte) 0x00);
//        ctxt.registerCategory(BaseCategories.NETWORK, (byte) 0x01);
//        ctxt.registerCategory(BaseCategories.PAYLOAD, (byte) 0x02);
//
//        //network category - no serializer registration required
//        ctxt.registerOpcode(BaseCategories.NETWORK, TEST_NET_REQUEST, MyNettyMsg.Request.class);
//        ctxt.registerOpcode(BaseCategories.NETWORK, TEST_NET_RESPONSE, MyNettyMsg.Response.class);
//        ctxt.registerOpcode(BaseCategories.NETWORK, TEST_NET_ONEWAY, MyNettyMsg.OneWay.class);
//
//        //bulk category
//        byte uuid = 0x01;
//        ctxt.registerOpcode(BaseCategories.BULK, uuid, UUID.class);
//        ctxt.registerSerializer(UUID.class, new UUIDSerializer());
//
//        //payloads
//        byte msgAReq = 0x01;
//        ctxt.registerOpcode(BaseCategories.PAYLOAD, msgAReq, MsgA.Request.class);
//        ctxt.registerSerializer(MsgA.Request.class, new MsgASerializer.Request());
//        byte msgAResp = 0x02;
//        ctxt.registerOpcode(BaseCategories.PAYLOAD, msgAResp, MsgA.Response.class);
//        ctxt.registerSerializer(MsgA.Response.class, new MsgASerializer.Response());
//        byte msgBReq = 0x03;
//        ctxt.registerOpcode(BaseCategories.PAYLOAD, msgBReq, MsgB.Request.class);
//        ctxt.registerSerializer(MsgB.Request.class, new MsgBSerializer.Request());
//        byte msgBResp = 0x04;
//        ctxt.registerOpcode(BaseCategories.PAYLOAD, msgBResp, MsgB.Response.class);
//        ctxt.registerSerializer(MsgB.Response.class, new MsgBSerializer.Response());
//
//        return ctxt;
//    }
//
//    @Override
//    protected RewriteableMsg decodeMsg(ChannelHandlerContext ctx, ByteBuf buffer) throws MessageDecodingException {
//
//        // See if msg is part of parent project, if yes then return it.
//        // Otherwise decode the msg here.
//        RewriteableMsg msg = super.decodeMsg(ctx, buffer);
//        if (msg != null) {
//            return msg;
//        }
//
//        switch (opKod) {
//            case TEST_NET_REQUEST:
//                return (new MyNettySerializer.Request()).myDecode(buffer);
//            case TEST_NET_RESPONSE:
//                return (new MyNettySerializer.Response()).myDecode(buffer);
//            case TEST_NET_ONEWAY:
//                return (new MyNettySerializer.OneWay()).myDecode(buffer);
//            default:
//                return null;
//        }
//    }
//
//    public SerializationContext getContext() {
//        return context;
//    }
//}
