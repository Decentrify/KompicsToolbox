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
package se.sics.ktoolbox.aggregator.global.example.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import se.sics.ktoolbox.aggregator.server.util.DesignInfoContainer;
import se.sics.ktoolbox.aggregator.server.util.DesignProcessor;
import se.sics.ktoolbox.aggregator.util.PacketInfo;

/**
 * The design processor to be created by the user 
 * based on the visualization requirement.
 *  
 * Created by babbarshaer on 2015-09-05.
 */
public class PseudoDesignProcessor implements DesignProcessor<PseudoPacketInfo, PseudoDesignInfo> {
    
    private Logger logger = LoggerFactory.getLogger(PseudoDesignProcessor.class);
    
    @Override
    public DesignInfoContainer<PseudoDesignInfo> process(Collection<Map<Integer, List<PacketInfo>>> windows) {
        
        logger.debug("Initiating the processing of the system information map.");
        
        Collection<PseudoDesignInfo> collectionResult = new ArrayList<PseudoDesignInfo>();
        for(Map<Integer, List<PacketInfo>> window : windows){
            
            int count = 0;
            float sum = 0;
            
            for(Map.Entry<Integer, List<PacketInfo>> entry: window.entrySet()){
                
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
