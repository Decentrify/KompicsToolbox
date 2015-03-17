package se.sics.p2ptoolbox.aggregator.api.model;

/**
 * Aggregated State information.
 *  
 * Created by babbarshaer on 2015-03-15.
 */

// NOTE: Ideally it should be an interface which will be extended by the users based on the information that they want to display.
public class AggregatedStatePacketImpl {

    private int indexEntries;
    private int partitioningDepth;
    private int partitionId;


    public AggregatedStatePacketImpl(int indexEntries, int partitioningDepth, int partitionId){
        this.indexEntries = indexEntries;
        this.partitionId = partitionId;
        this.partitioningDepth = partitioningDepth;
    }

    public String toString(){
        return " IndexEntries: " +indexEntries+ " Partitioning Depth: " +partitioningDepth+ " PartitionId: " + partitionId;
    }

    public int getIndexEntries() {
        return indexEntries;
    }

    public int getPartitioningDepth() {
        return partitioningDepth;
    }

    public int getPartitionId() {
        return partitionId;
    }
    
}
