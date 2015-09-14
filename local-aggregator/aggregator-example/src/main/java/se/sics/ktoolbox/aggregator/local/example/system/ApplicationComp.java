package se.sics.ktoolbox.aggregator.local.example.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.local.api.events.ComponentInfoEvent;
import se.sics.ktoolbox.aggregator.local.api.ports.LocalAggregatorPort;
import se.sics.ktoolbox.aggregator.local.example.util.AppComponentInfo;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * Main application used to test the local aggregator application.
 * Created by babbar on 2015-08-31.
 */
public class ApplicationComp extends ComponentDefinition {


    private Logger logger = LoggerFactory.getLogger(ApplicationComp.class);
    private DecoratedAddress selfAddress;

    private Integer t1;
    private Integer t2;
    private String str;

    private static final int OVERLAY_IDENTIFIER = 0;

    Positive<LocalAggregatorPort> aggregatorPort = requires(LocalAggregatorPort.class);
    Positive<Timer> timer = requires(Timer.class);

    public ApplicationComp(ApplicationCompInit init){

        doInit(init);
        subscribe(startHandler, control);
        subscribe(appTimeoutHandler, timer);
    }

    private void doInit(ApplicationCompInit init) {

        logger.debug("Initializing the component.");
        this.selfAddress = init.selfAddress;
        this.t1 = 0;
        this.t2 = 0;
        this.str = "My Application.";
    }



    Handler<Start> startHandler =  new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component is being booted up.");

            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(4000, 4000);
            ApplicationTimeout timeout = new ApplicationTimeout(spt);

            spt.setTimeoutEvent(timeout);
            trigger(spt, timer);
        }
    };



    Handler<ApplicationTimeout> appTimeoutHandler = new Handler<ApplicationTimeout>() {
        @Override
        public void handle(ApplicationTimeout applicationTimeout) {

            logger.debug("Sending component information to local aggregator");

            AppComponentInfo info = new AppComponentInfo(t1.intValue(), t2.intValue(), str);
            logger.debug("Component Information Created :{} ", info);

            trigger(new ComponentInfoEvent(OVERLAY_IDENTIFIER, info), aggregatorPort);

            t1 = new Integer(t1.intValue() + 1);
            t2 = new Integer(t2.intValue() + 1);
        }
    };





    private class ApplicationTimeout extends Timeout{

        public ApplicationTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }

}
