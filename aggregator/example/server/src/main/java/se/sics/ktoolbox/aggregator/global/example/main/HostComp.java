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
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.global.example.PseudoGlobalAggregator;
import se.sics.ktoolbox.aggregator.global.example.system.AnotherDesignInfoContainer;
import se.sics.ktoolbox.aggregator.global.example.system.PseudoDesignInfoContainer;
import se.sics.ktoolbox.aggregator.global.example.system.PseudoDesignProcessor;
import se.sics.ktoolbox.aggregator.global.example.util.DesignerEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.ktoolbox.aggregator.server.GlobalAggregatorPort;
import se.sics.ktoolbox.aggregator.server.Visualizer;
import se.sics.ktoolbox.aggregator.server.VisualizerInit;
import se.sics.ktoolbox.aggregator.server.VisualizerPort;
import se.sics.ktoolbox.aggregator.server.event.WindowProcessing;
import se.sics.ktoolbox.aggregator.server.util.DesignProcessor;

/**
 * Host Component enclosing the main components.
 * 
 * Created by babbarshaer on 2015-09-05.
 */
public class HostComp extends ComponentDefinition{
    
    private Logger logger = LoggerFactory.getLogger(HostComp.class);
    private Component pseudoAggregator;
    private Component visualizer;

    Positive<Timer> timer = requires(Timer.class);
    
    public HostComp() {
        
        logger.debug("Initialized the component");
        
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        
    }
   
    
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            
            logger.debug("Component started.");
            doStart();
            
            ScheduleTimeout st = new ScheduleTimeout(10000);
            OneTimeTimeout ott = new OneTimeTimeout(st);
            st.setTimeoutEvent(ott);
            
            trigger(st, timer);
        }
    };

    
    /**
     * Trigger start to the components.
     */
    private void doStart(){
        
        logger.debug("Creating child components.");
        int maxSnapShots = 10;
        
        Map<String, DesignProcessor> processorMap = new HashMap<String, DesignProcessor>();
        processorMap.put(DesignerEnum.PSEUDO.getVal(), new PseudoDesignProcessor());
        
        pseudoAggregator = create(PseudoGlobalAggregator.class, Init.NONE);
        visualizer = create(Visualizer.class, new VisualizerInit(maxSnapShots, processorMap));
        connect(pseudoAggregator.getNegative(Timer.class), timer);
        connect(visualizer.getNegative(GlobalAggregatorPort.class), pseudoAggregator.getPositive(GlobalAggregatorPort.class));
        
        trigger(Start.event, pseudoAggregator.getControl());
        trigger(Start.event, visualizer.getControl());

        subscribe(designHandler, visualizer.getPositive(VisualizerPort.class));
        subscribe(anotherDesignHandler, visualizer.getPositive(VisualizerPort.class));
    }
    
    
    ClassMatchedHandler<PseudoDesignInfoContainer, WindowProcessing.Response<PseudoDesignInfoContainer>> designHandler = new ClassMatchedHandler<PseudoDesignInfoContainer, WindowProcessing.Response<PseudoDesignInfoContainer>>() {
        @Override
        public void handle(PseudoDesignInfoContainer pseudoDesignInfoContainer, WindowProcessing.Response<PseudoDesignInfoContainer> event) {
            
            logger.debug("Handler is being hit.");
            
            PseudoDesignInfoContainer container = event.getContent();
            logger.debug("{}", container);
        }
    };


    ClassMatchedHandler<AnotherDesignInfoContainer, WindowProcessing.Response<AnotherDesignInfoContainer>> anotherDesignHandler = new ClassMatchedHandler<AnotherDesignInfoContainer, WindowProcessing.Response<AnotherDesignInfoContainer>>() {
        @Override
        public void handle(AnotherDesignInfoContainer content, WindowProcessing.Response<AnotherDesignInfoContainer> context) {
            logger.debug("Handler for the another design info container");
        }
    };
    
    
    Handler<OneTimeTimeout> timeoutHandler = new Handler<OneTimeTimeout>() {
        @Override
        public void handle(OneTimeTimeout oneTimeTimeout) {
            
            logger.debug("Need to request for window processing to the visualizer.");
            WindowProcessing.Request request = new WindowProcessing.Request(UUID.randomUUID(), DesignerEnum.PSEUDO.getVal(), 0, 1);
            trigger(request, visualizer.getPositive(VisualizerPort.class));
        }
    };
    
    
    public class OneTimeTimeout extends Timeout{

        public OneTimeTimeout(ScheduleTimeout request) {
            super(request);
        }
    }
    
}
