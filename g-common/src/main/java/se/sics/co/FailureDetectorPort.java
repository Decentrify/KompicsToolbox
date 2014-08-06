/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.co;

import java.util.HashSet;
import se.sics.gvod.net.VodAddress;
import se.sics.kompics.Event;
import se.sics.kompics.PortType;

/**
 *
 * @author alidar
 */
public class FailureDetectorPort extends PortType {
    {
        request(FailureDetectorEvent.class);
        indication(FailureDetectorEvent.class);
    }
    
    public static class FailureDetectorEvent extends Event{
    
        private HashSet<VodAddress> suspectedNodes = new HashSet<>();

        public FailureDetectorEvent(VodAddress suspectedNode){
            
            suspectedNodes.add(suspectedNode);
        }

        public FailureDetectorEvent(HashSet<VodAddress> suspectedNodes) {
            this.suspectedNodes = suspectedNodes;
        }

        /**
         * @return the suspectedNodes
         */
        public HashSet<VodAddress> getSuspectedNodes() {
            return suspectedNodes;
        }

        /**
         * @param suspectedNodes the suspectedNodes to set
         */
        public void setSuspectedNodes(HashSet<VodAddress> suspectedNodes) {
            this.suspectedNodes = suspectedNodes;
        }
    
    }
}
