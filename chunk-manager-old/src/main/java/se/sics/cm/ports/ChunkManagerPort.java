package se.sics.cm.ports;

import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.kompics.Event;
import se.sics.kompics.PortType;

/**
 * Created by alidar on 10/21/14.
 */
public class ChunkManagerPort extends PortType {
    {
        request(DirectMsg.class);
        indication(DirectMsg.class);
    }
}
