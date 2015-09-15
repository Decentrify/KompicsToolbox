package se.sics.ktoolbox.aggregator.global.example.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.aggregator.common.PacketInfo;
import se.sics.ktoolbox.aggregator.server.api.system.DesignInfoContainer;
import se.sics.ktoolbox.aggregator.server.api.system.DesignProcessor;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The design processor to be created by the user 
 * based on the visualization requirement.
 *  
 * Created by babbarshaer on 2015-09-05.
 */
public class PseudoDesignProcessor implements DesignProcessor<PseudoPacketInfo, PseudoDesignInfo> {
    
    private Logger logger = LoggerFactory.getLogger(PseudoDesignProcessor.class);
    
    @Override
    public DesignInfoContainer<PseudoDesignInfo> process(Collection<Map<BasicAddress, List<PacketInfo>>> windows) {
        
        logger.debug("Initiating the processing of the system information map.");
        
        Collection<PseudoDesignInfo> collectionResult = new ArrayList<PseudoDesignInfo>();
        for(Map<BasicAddress, List<PacketInfo>> window : windows){
            
            int count = 0;
            float sum = 0;
            
            for(Map.Entry<BasicAddress, List<PacketInfo>> entry: window.entrySet()){
                
                for(PacketInfo info : entry.getValue()){
                    if( info instanceof  PseudoPacketInfo){

                        count ++;
                        PseudoPacketInfo ppi = (PseudoPacketInfo) info;
                        sum += ppi.getResponse();
                    }
                }
            }
            
            if(count > 0){
                float average = sum / count;
                PseudoDesignInfo psdi = new PseudoDesignInfo(average, count);
                collectionResult.add(psdi);
            }
        }
        
        return new PseudoDesignInfoContainer(collectionResult);
    }

    @Override
    public void cleanState() {
        logger.info("Clear local state if any.");
    }
}
