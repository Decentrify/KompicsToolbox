package se.sics.p2ptoolbox.aggregator.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.aggregator.api.msg.GlobalState;
import se.sics.p2ptoolbox.aggregator.api.msg.Ready;
import se.sics.p2ptoolbox.aggregator.api.port.GlobalAggregatorPort;
import se.sics.p2ptoolbox.aggregator.core.GlobalAggregatorComponent;
import se.sics.p2ptoolbox.aggregator.core.GlobalAggregatorComponentInit;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.sql.Time;

/**
 * Main Application Component which would receive the updates from the Aggregator Component.
 *
 * Created by babbar on 2015-03-17.
 */
public class Application extends ComponentDefinition{

    private Logger logger = LoggerFactory.getLogger(Application.class);
    Positive<Timer> timerPositive = requires(Timer.class);
    Positive<Network> networkPositive = requires(Network.class);
    Negative<ApplicationPort> applicationPortNegative = provides(ApplicationPort.class);

    Component aggregator;

    public Application(){

        aggregator = create(GlobalAggregatorComponent.class, new GlobalAggregatorComponentInit(5000, 10000));
        connect(aggregator.getNegative(Network.class), networkPositive);
        connect(aggregator.getNegative(Timer.class), timerPositive);

        subscribe(startHandler, control);
        subscribe(globalStateHandler, aggregator.getPositive(GlobalAggregatorPort.class));
        subscribe(readyHandler, aggregator.getPositive(GlobalAggregatorPort.class));
    }


    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            logger.info("Main Application Component Started.");
        }
    };

    Handler<GlobalState> globalStateHandler = new Handler<GlobalState>() {
        @Override
        public void handle(GlobalState globalState) {
            logger.info("Received the global state update with length: " + globalState.getStatePacketMap().size());
        }
    };

    Handler<Ready> readyHandler = new Handler<Ready>() {
        @Override
        public void handle(Ready ready) {
            logger.info("Received Ready event from the aggregator component");
            logger.info("Triggering own ready event.");
            trigger(new Ready(), applicationPortNegative);
        }
    };




    public static class ApplicationInit extends Init<Application>{

        public DecoratedAddress aggregatorAddress;
        public ApplicationInit(DecoratedAddress aggregatorAddress){
            this.aggregatorAddress = aggregatorAddress;
        }

    }

}
