package se.sics.p2ptoolbox.aggregator.example.core;

import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;

/**
 * Packet Sample Implementation.
 *
 * Created by babbarshaer on 2015-03-17.
 */
public class PacketSample implements AggregatedStatePacket{

    public int partitionDepth;
    public int indexEntries;
    public int partitionId;
    public int nodeId;

    public PacketSample(int partitionDepth, int indexEntries, int partitionId, int nodeId) {
        this.partitionDepth = partitionDepth;
        this.indexEntries = indexEntries;
        this.partitionId = partitionId;
        this.nodeId = nodeId;
    }
    
    
    public String toString(){
        return "PACKET SAMPLE: " + " Id: "+ nodeId + " Partition Depth: " + partitionDepth + " Index Entries: " + indexEntries + " Partition ID: " + partitionId;
    }
}
