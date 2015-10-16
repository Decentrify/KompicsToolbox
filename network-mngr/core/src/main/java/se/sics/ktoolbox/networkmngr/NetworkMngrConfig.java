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
package se.sics.ktoolbox.networkmngr;

import com.google.common.base.Optional;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption;
import se.sics.p2ptoolbox.util.config.KConfigOption.Basic;
import se.sics.p2ptoolbox.util.config.options.InetAddressOption;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkMngrConfig implements KConfigLevel {

    private final static Basic<String> prefferedInterface = new Basic("network.prefferedInterface", String.class, new NetworkMngrConfig());
    private final static Basic<List<String>> prefferedInterfaces = new Basic("network.prefferedInterfaces", List.class, new NetworkMngrConfig());

    public final static InetAddressOption prefferedIp = new InetAddressOption("network.prefferedIp", InetAddress.class, new NetworkMngrConfig(), prefferedInterface);
    public final static InterfaceMasksOption prefferedMasks = new InterfaceMasksOption("network.prefferedMasks", List.class, new NetworkMngrConfig());

    public static void register(KConfigCore config) {
        config.define(prefferedInterface);
        config.define(prefferedInterfaces);
    }

    @Override
    public Set<String> canWrite() {
        Set<String> canWrite = new HashSet<>();
        canWrite.add(toString());
        return canWrite;
    }

    @Override
    public String toString() {
        return "NetworkMngrConfig";
    }

    public static class InterfaceMasksOption extends KConfigOption.Composite<List> {

        public InterfaceMasksOption(String name, Class<List> type, KConfigLevel lvl) {
            super(name, type, lvl);
        }

        @Override
        public Optional<List> read(KConfigCache config) {
            Optional<List<String>> sPrefferedInterfaces = config.read(prefferedInterfaces);
            if (!sPrefferedInterfaces.isPresent()) {
                return Optional.absent();
            }
            List<GetIp.NetworkInterfacesMask> masks = new ArrayList<>();
            for (String prefInt : sPrefferedInterfaces.get()) {
                switch (prefInt) {
                    case "PUBLIC":
                        masks.add(GetIp.NetworkInterfacesMask.PUBLIC);
                        break;
                    case "PRIVATE":
                        masks.add(GetIp.NetworkInterfacesMask.PRIVATE);
                        break;
                    case "TENDOT":
                        masks.add(GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE);
                        break;
                    default: 
                        throw new RuntimeException("unknown:" + prefInt);
                }
            }
            return Optional.of((List)masks);
        }
    }
}
