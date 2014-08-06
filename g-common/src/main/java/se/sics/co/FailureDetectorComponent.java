/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.co;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;

/**
 *
 * @author alidar
 */
public class FailureDetectorComponent extends ComponentDefinition{
    
    Negative<FailureDetectorPort> fdPort = provides(FailureDetectorPort.class);
    
    public FailureDetectorComponent() {
        subscribe(handleFD, fdPort);
    }
    
    public Handler<FailureDetectorPort.FailureDetectorEvent> handleFD = new Handler<FailureDetectorPort.FailureDetectorEvent>() {

        @Override
        public void handle(FailureDetectorPort.FailureDetectorEvent event) {
            trigger(event, fdPort);
        }
    };    
    
}
