package se.sics.ktoolbox.aggregator.global.example.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

/**
 * Launcher component responsible for setting up the connections.
 *
 * Created by babbarshaer on 2015-09-05.
 */
public class Launcher extends ComponentDefinition {
    
    private Logger logger = LoggerFactory.getLogger(Launcher.class);
    Component timer;
    Component hostComp;
    
    public Launcher(){
        
        logger.debug("Component booted up.");
        subscribe(startHandler, control);
    }
    
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            
            logger.debug("Component started.");
            
            timer = create(JavaTimer.class, Init.NONE);
            hostComp = create(HostComp.class, Init.NONE);
            
            connect(hostComp.getNegative(Timer.class), timer.getPositive(Timer.class));
            
            trigger(Start.event, hostComp.control());
            trigger(Start.event, timer.control());
        }
    };
    
}
