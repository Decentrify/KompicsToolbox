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
package se.sics.ledbat.system;

import java.util.Random;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayRegistry;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ledbat.LedbatSerializerSetup;
import se.sics.ledbat.core.LedbatConfig;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SeederLauncher extends ComponentDefinition {
    public SeederLauncher() {
        systemSetup();
        
        SeederLauncherConfig seederConfig = new SeederLauncherConfig(config());
        LedbatConfig ledbatConfig = new LedbatConfig(config());
        
        Component network = create(NettyNetwork.class, new NettyInit(seederConfig.self));
        Random rand = new Random(1);
        byte[] payload = new byte[ledbatConfig.mss];
        rand.nextBytes(payload);
        Component seeder = create(Seeder.class, new Seeder.Init(payload));
        connect(seeder.getNegative(Network.class), network.getPositive(Network.class), Channel.TWO_WAY);
    }
    
    private void systemSetup() {
        overlaysSetup();
        serializersSetup();
    }
    
    private void overlaysSetup() {
        OverlayRegistry.initiate(new OverlayId.BasicTypeFactory((byte)0), new OverlayId.BasicTypeComparator());
        
        byte torrentOwnerId = 1;
        OverlayRegistry.registerPrefix(TorrentIds.TORRENT_OVERLAYS, torrentOwnerId);
        
        IdentifierFactory torrentBaseIdFactory = IdentifierRegistry.lookup(BasicIdentifiers.Values.OVERLAY.toString());
        torrentIdFactory = new OverlayIdFactory(torrentBaseIdFactory, TorrentIds.Types.TORRENT, torrentOwnerId);
    }
    
    private void serializersSetup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = LedbatSerializerSetup.registerSerializers(serializerId);
        serializerId = ExSerializerSetup.registerSerializers(serializerId);
    }
    
    public static void main(String[] args) {
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(SeederLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
