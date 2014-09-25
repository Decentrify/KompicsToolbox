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

import java.net.InetAddress;
import java.net.UnknownHostException;
import se.sics.gvod.address.Address;
import se.sics.gvod.net.MsgFrameDecoder;
import se.sics.gvod.net.Transport;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class TestSetup {
    public static int seed = 1234;
    public static Address selfAddress;
    public static Address partnerAddress;
    public static Class<? extends MsgFrameDecoder> msgFrameDecoderClass;
    public static Transport transport;

    static {
        try {
            InetAddress selfIp = InetAddress.getByName("193.10.66.247");
            selfAddress = new Address(selfIp, 30000, 234);
            
            InetAddress partnerIp = InetAddress.getByName("193.10.67.178");
            partnerAddress = new Address(partnerIp, 20000, 123);
            
            msgFrameDecoderClass = SystemFrameDecoder.class;
            
            transport = Transport.UDP;
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }
}
