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
import se.sics.ktoolbox.aggregator.server.api.event.WindowProcessing;
import se.sics.ktoolbox.aggregator.server.api.ports.GlobalAggregatorPort;
import se.sics.ktoolbox.aggregator.server.api.ports.VisualizerPort;
import se.sics.ktoolbox.aggregator.server.api.system.DesignProcessor;
import se.sics.ktoolbox.aggregator.server.core.Visualizer;
import se.sics.ktoolbox.aggregator.server.core.VisualizerInit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
