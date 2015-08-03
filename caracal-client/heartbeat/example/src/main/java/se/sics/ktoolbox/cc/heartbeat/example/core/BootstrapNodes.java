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

package se.sics.ktoolbox.cc.heartbeat.example.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import se.sics.caracaldb.Address;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapNodes {
    public static List<Address> readCaracalBootstrap(Config config) {
        try {
            ArrayList<Address> cBootstrap = new ArrayList<Address>();
            InetAddress ip = InetAddress.getByName(config.getString("caracal.address.ip"));
            int port = config.getInt("caracal.address.port");
            cBootstrap.add(new Address(ip, port, null));
            return cBootstrap;
        } catch (ConfigException.Missing ex) {
            throw new RuntimeException("Caracal Bootstrap configuration problem - missing config", ex);
        } catch (UnknownHostException ex) {
            throw new RuntimeException("Caracal Bootstrap configuration problem - bad ip", ex);
        }
    }
}
