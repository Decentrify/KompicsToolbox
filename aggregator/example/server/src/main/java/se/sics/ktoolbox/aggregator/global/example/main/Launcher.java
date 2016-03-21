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
