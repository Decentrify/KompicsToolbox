package se.sics.p2ptoolbox.gradient.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.co.IndividualTimeout;
import se.sics.gvod.common.Self;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.*;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.*;
import se.sics.p2ptoolbox.croupier.api.CroupierPort;
import se.sics.p2ptoolbox.croupier.api.msg.CroupierSample;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.gradient.api.msg.GradientJoin;
import se.sics.p2ptoolbox.gradient.api.msg.GradientSample;
import se.sics.p2ptoolbox.gradient.api.msg.GradientUpdate;
import se.sics.p2ptoolbox.gradient.api.ports.GradientControlPort;
import se.sics.p2ptoolbox.gradient.api.ports.GradientPort;
import se.sics.p2ptoolbox.gradient.api.util.GradientHelper;
import se.sics.p2ptoolbox.gradient.api.util.GradientPeerView;
import se.sics.p2ptoolbox.gradient.msg.GradientShuffleMessage;

import java.util.*;
import java.util.UUID;

/**
 *
 * Main Gradient class responsible for shuffling peer views with neighbours.
 * It is responsible for maintaining the gradient and returning periodically gradient sample,
 * to the application. 
 *  
 */
public class Gradient extends ComponentDefinition{
    
    // == Declare variables.
    private Logger logger = LoggerFactory.getLogger(Gradient.class);
    private Self self;
    private VodAddress selfAddress;
    private GradientConfig config;
    private GradientHelper<? extends PeerView> gradientHelper;
    private Random random;
    private String compName;
    private Map<se.sics.gvod.timer.UUID, VodAddress> outstandingShuffles;
    private PeerView selfPeerView;
    
    // == Identify Ports.
    Negative<GradientControlPort> gradientControlPort = provides(GradientControlPort.class);
    Negative<GradientPort> gradientPort = provides(GradientPort.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<CroupierPort> croupierPort = requires(CroupierPort.class);
    
    
    public Gradient(GradientInit init){
        
        doInit(init);
        
        subscribe(startHandler, control);
        subscribe(gradientJoinEventHandler, gradientControlPort);
        subscribe(gradientRoundHandler, timer);
        subscribe(gradientShuffleRequestHandler, network);
        subscribe(gradientShuffleResponseHandler, network);
        subscribe(croupierSampleHandler, croupierPort);
        subscribe(gradientUpdateHandler, gradientPort);
    }

    /**
     * Timeout to periodically issue exchanges.
     */
    public class GradientRound extends IndividualTimeout {
        
        public GradientRound(SchedulePeriodicTimeout request, int id) {
            super(request, id);
        }
    }
    
    
    
    /**
     *  Perform initialization tasks for the Gradient service.
     * @param init Init
     */
    private void doInit(GradientInit init) {
        
        logger.debug("Gradient component init called .... ");
        
        self = init.getSelf();
        selfAddress = self.getAddress();
        config  = init.getGradientConfig();
        gradientHelper = init.getGradientHelper();
        
        random = new Random(config.getSeed());
        compName = "(" + self.getId() + ", " + self.getOverlayId() + ") ";
        outstandingShuffles = new HashMap<se.sics.gvod.timer.UUID, VodAddress>();
        
    }
    
    
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Start Handler invoked.");
        }
    };

    /**
     * TODO: Formulate proper functionality for scheduling.
     * Handler for the gradient join event, which schedules a timeout event
     * in case we received nodes in the bootstrap address.
     */
    Handler<GradientJoin> gradientJoinEventHandler = new Handler<GradientJoin>(){
        
        @Override
        public void handle(GradientJoin event) {
            
            logger.debug("{} : Gradient join event received.", self.getId());
            
            Set<VodAddress> bootstrapAddressSet = event.getBootstrapNodes();
            if(bootstrapAddressSet == null || bootstrapAddressSet.isEmpty()){
                return;
            }
            
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.getShufflePeriod(), config.getShufflePeriod());
            spt.setTimeoutEvent(new GradientRound(spt,self.getId()));
            trigger(spt, timer);
        }
    };


    /**
     * Handle Gradient Round. When gradient wakes up, check for empty gradient,
     * and based on the policy shuffle with the peer.
     *
     */
    Handler<GradientRound> gradientRoundHandler = new Handler<GradientRound>() {
        @Override
        public void handle(GradientRound event) {
            
            if(!gradientHelper.isSampleSetEmpty()){
                initiateShuffle(gradientHelper.selectPeerToShuffleWith());
            }
            
            // TODO: Not sure what the implementation needs to be at this time, in case set empty.
            
            // Fix 1: Close everything down and wait till you get another
            // FIX 2: Keep everything going and in every round, check if you have any neighbors. ( This needs to be choice for the Network Partitioning. )
        }
    };

    
    /**
     * Croupier Sample received by the gradient needs to be processed by Gradient.
     */
    Handler<CroupierSample> croupierSampleHandler = new Handler<CroupierSample>() {
        @Override
        public void handle(CroupierSample event) {
            
            logger.debug("{}: Received Croupier Sample.", self.getId());
            
            List<CroupierPeerView> croupierPeerViewList = new ArrayList<CroupierPeerView>();
            croupierPeerViewList.addAll(event.publicSample);
            croupierPeerViewList.addAll(event.privateSample);
            
            gradientHelper.mergeCroupierExchangeSample(croupierPeerViewList);
        }
    };
    
    

    /**
     * Based on the shuffle policy, select a neighbor to shuffle with.
     * @param neighbor Neighbor to shuffle with.
     *
     */
    private void initiateShuffle(GradientPeerView neighbor){
        
        Set<GradientPeerView> exchangeNodes = gradientHelper.getNearestNodes(neighbor);
        exchangeNodes.add(new GradientPeerView(selfPeerView, self.getAddress()));

        ScheduleTimeout scheduleTimeout = new ScheduleTimeout(config.getShufflePeriod());
        scheduleTimeout.setTimeoutEvent(new GradientShuffleMessage.Timeout(scheduleTimeout, self.getId()));

        se.sics.gvod.timer.UUID rTimeoutId = (se.sics.gvod.timer.UUID)scheduleTimeout.getTimeoutEvent().getTimeoutId();
        outstandingShuffles.put(rTimeoutId, neighbor.src);
        
        trigger(new GradientShuffleMessage.Request(self.getAddress(), neighbor.src, rTimeoutId, exchangeNodes), network);
        trigger(scheduleTimeout, timer);
    }
    
    
    
    Handler<GradientShuffleMessage.Request> gradientShuffleRequestHandler = new Handler<GradientShuffleMessage.Request>() {
        @Override
        public void handle(GradientShuffleMessage.Request event) {
            
            logger.debug("{}: Shuffle request received from: {}", self.getAddress(), event.getSource());
            
            
            GradientPeerView neighborPeerView = null;
            
            for(GradientPeerView gpv : event.getExchangeNodes()){
                if(gpv.src == event.getVodSource()){
                    neighborPeerView = gpv;
                    break;
                }
            }
            
            if(neighborPeerView == null){
                logger.warn("{}: Neighbor: {} tried to shuffle without following protocol. Not Responding ..", self.getAddress(), event.getVodSource());
                return;
            }


            Set<GradientPeerView> responseNodes = gradientHelper.getNearestNodes(neighborPeerView);
            GradientShuffleMessage.Response response = new GradientShuffleMessage.Response(selfAddress, event.getVodSource(),event.getTimeoutId(),responseNodes);
            trigger(response, network);
            
            gradientHelper.mergeGradientExchangeSample(event.getExchangeNodes());
            gradientHelper.incrementSamplesAge();

            trigger(new GradientSample(java.util.UUID.randomUUID(),gradientHelper.getSampleSet(), gradientHelper.isConverged()), gradientPort);
        }
    };

    
    
    Handler<GradientShuffleMessage.Response> gradientShuffleResponseHandler = new Handler<GradientShuffleMessage.Response>() {
        @Override
        public void handle(GradientShuffleMessage.Response event) {
            
            se.sics.gvod.timer.UUID shuffleId = (se.sics.gvod.timer.UUID) event.getTimeoutId();
            logger.debug("{}: Received Gradient shuffle response from: {}", selfAddress, event.getVodSource());
            
            if(outstandingShuffles.containsKey(shuffleId)){
                
                outstandingShuffles.remove(shuffleId);
                CancelTimeout ct= new CancelTimeout(shuffleId);
                trigger(ct, timer);
            }
            
            // TODO: Each component has a responsibility of keeping a copy of its sample set.
            gradientHelper.mergeGradientExchangeSample(event.getExchangeNodes());
            trigger(new GradientSample(java.util.UUID.randomUUID(),gradientHelper.getSampleSet(), gradientHelper.isConverged()), gradientPort);
        }
    };
    
    
    
    Handler<GradientUpdate> gradientUpdateHandler = new Handler<GradientUpdate>() {
        @Override
        public void handle(GradientUpdate event) {
            selfPeerView = event.getPeerView() == null ? selfPeerView : event.getPeerView();
            gradientHelper.handleSelfViewUpdate(selfPeerView);
        }
    };
    

}
