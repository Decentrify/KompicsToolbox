package se.sics.p2ptoolbox.election.example.msg;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Collection;

/**
 * Message sent by the simulator to the application informing about the peers in the system.
 *
 * Created by babbar on 2015-04-01.
 */
public class AddPeers{

    public Collection<DecoratedAddress> peers;
    public AddPeers(Collection<DecoratedAddress> peers){
        this.peers = peers;
    }
}
