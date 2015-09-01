package se.sics.ktoolbox.aggregator.global.api;

import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.UUID;

/**
 * Container for the packet information which is sent
 * by the application to the global container.
 *
 * Created by babbarshaer on 2015-09-01.
 */
public class PacketContainer {

    public final UUID uuid;
    public final  DecoratedAddress sourceAddress;
    public final  PacketInfo packetInfo;

    public PacketContainer(UUID uuid, DecoratedAddress sourceAddress, PacketInfo packetInfo){

        this.uuid = uuid;
        this.sourceAddress = sourceAddress;
        this.packetInfo = packetInfo;
    }


}
