package se.sics.ktoolbox.aggregator.global.api.event;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.aggregator.global.api.system.PacketInfo;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.List;
import java.util.Map;

/**
 * Event from the aggregator indicating the
 * aggregated information from the nodes in the system.
 *
 * Created by babbarshaer on 2015-09-02.
 */
public class AggregatedInfo implements KompicsEvent {

    private final Map<BasicAddress, List<PacketInfo>> nodePacketMap;

    public AggregatedInfo(Map<BasicAddress, List<PacketInfo>> nodePacketMap){
        this.nodePacketMap = nodePacketMap;
    }

    public Map<BasicAddress, List<PacketInfo>> getNodePacketMap() {
        return nodePacketMap;
    }
}
