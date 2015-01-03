package se.sics.p2ptoolbox.croupier.core;

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


import java.util.HashSet;
import java.util.Set;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;
import se.sics.p2ptoolbox.croupier.core.msg.Shuffle;
import se.sics.p2ptoolbox.croupier.core.msg.ShuffleNet;
import se.sics.p2ptoolbox.croupier.core.net.CroupierPVSerializer;
import se.sics.p2ptoolbox.croupier.core.net.ShuffleNetSerializer;
import se.sics.p2ptoolbox.croupier.core.net.ShuffleSerializer;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.msg.NetMsg;
import se.sics.p2ptoolbox.serialization.msg.OverlayHeaderField;
import se.sics.p2ptoolbox.serialization.serializer.SerializerAdapter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierNetworkSettings {

    private static SerializationContext context = null;

    public static void setContext(SerializationContext setContext) {
        context = setContext;
    }

    public static boolean hasContext() {
        return context != null;
    }
    
    public static SerializationContext getContext() {
        return context;
    }

    public static boolean checkPreCond() {
        if (context == null || !NetMsg.hasContext() || !SerializerAdapter.hasContext()) {
            return false;
        }
        
        try {
            for(CroupierConfig.OtherSerializers serializedClass : CroupierConfig.OtherSerializers.values()) {
               context.getSerializer(serializedClass.serializedClass);
            }
        } catch (SerializationContext.MissingException ex) {
            throw new RuntimeException(ex);
        }
        
        Set<String> croupierAliases = new HashSet<String>();
        croupierAliases.add(CroupierConfig.MsgAliases.CROUPIER_NET_REQUEST.toString());
        croupierAliases.add(CroupierConfig.MsgAliases.CROUPIER_NET_RESPONSE.toString());
        croupierAliases.add(CroupierConfig.OtherAliases.HEADER_FIELD.toString());
        croupierAliases.add(CroupierConfig.OtherAliases.PEER_VIEW.toString());
        return context.containsAliases(croupierAliases);
    }

    public static void registerSerializers() {
        registerNetworkMsg();
        registerOthers();
    }

    private static void registerNetworkMsg() {
        try {
            context.registerSerializer(ShuffleNet.Request.class, new ShuffleNetSerializer.Request());
            context.registerSerializer(ShuffleNet.Response.class, new ShuffleNetSerializer.Response());
            
            context.multiplexAlias(CroupierConfig.MsgAliases.CROUPIER_NET_REQUEST.toString(), ShuffleNet.Request.class, (byte) 0x01);
            context.multiplexAlias(CroupierConfig.MsgAliases.CROUPIER_NET_RESPONSE.toString(), ShuffleNet.Response.class, (byte) 0x01);
        } catch (SerializationContext.DuplicateException ex) {
            throw new RuntimeException(ex);
        } catch (SerializationContext.MissingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static void registerOthers() {
        try {
            context.registerSerializer(Shuffle.class, new ShuffleSerializer());
            context.registerSerializer(CroupierPeerView.class, new CroupierPVSerializer());
        } catch (SerializationContext.DuplicateException ex) {
            throw new RuntimeException(ex);
        }
    }
}
