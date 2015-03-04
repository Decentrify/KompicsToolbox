/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.gradient.core;

import se.sics.p2ptoolbox.gradient.core.util.GradientView;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.co.FailureDetectorPort;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.p2ptoolbox.croupier.api.CroupierPort;
import se.sics.p2ptoolbox.croupier.api.msg.CroupierSample;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.gradient.api.msg.GradientSample;
import se.sics.p2ptoolbox.gradient.api.msg.GradientUpdate;
import se.sics.p2ptoolbox.gradient.api.GradientControlPort;
import se.sics.p2ptoolbox.gradient.api.GradientPort;
import se.sics.p2ptoolbox.gradient.msg.Shuffle;
import se.sics.p2ptoolbox.gradient.msg.ShuffleNet;

/**
 *
 * Main Gradient class responsible for shuffling peer views with neighbors. It
 * is responsible for maintaining the gradient and returning periodically
 * gradient sample, to the application.
 *
 */
public class Gradient extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(Gradient.class);

    // == Declare variables.
    private final VodAddress selfAddress;
    private final GradientConfig config;
    private final int overlayId;

    private CroupierPeerView selfCPV;
    private final GradientView view;
    private TimeoutId shuffleTId;
    private ShuffleNet.Request outstandingShuffle;

    // == Identify Ports.
    Negative<GradientControlPort> gradientControlPort = provides(GradientControlPort.class);
    Negative<GradientPort> gradientPort = provides(GradientPort.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<CroupierPort> croupierPort = requires(CroupierPort.class);
    Positive<FailureDetectorPort> failurePropagationPort = requires(FailureDetectorPort.class);

    public Gradient(GradientInit init) {
        this.selfAddress = init.selfAddress;
        this.config = init.gradientConfig;
        this.overlayId = init.overlayId;

        logger.debug("<{}/{}> initializing...", selfAddress.getId(), overlayId);
        this.view = new GradientView(init.utilityComparator, config.viewSize, new Random(init.seed));

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        subscribe(handleUpdate, gradientPort);
        subscribe(handleNodeFailure, failurePropagationPort);
        subscribe(handleCroupierSample, croupierPort);
        subscribe(handleShuffle, timer);
        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);

    }

    //*********Control**********************************************************
    Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("<{}/{}> starting...", selfAddress.getId(), overlayId);

            logger.debug("<{}/{}> scheduling periodic shuffle timeout", selfAddress.getId(), overlayId);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.shufflePeriod, config.shufflePeriod);
            ShuffleTimeout shuffleT = new ShuffleTimeout(spt);
            spt.setTimeoutEvent(shuffleT);
            trigger(spt, timer);
            shuffleTId = shuffleT.getTimeoutId();
        }
    };

    Handler<Stop> handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            logger.debug("<{}/{}> stopping...", selfAddress.getId(), overlayId);
        }
    };
    //**************************************************************************
    Handler<GradientUpdate> handleUpdate = new Handler<GradientUpdate>() {
        @Override
        public void handle(GradientUpdate event) {
            logger.debug("<{}/{}> updating self peer view:{}", new Object[]{selfAddress.getId(), overlayId, event.peerView});
            selfCPV = new CroupierPeerView(event.peerView, selfAddress);
        }
    };
    
    Handler<FailureDetectorPort.FailureDetectorEvent> handleNodeFailure = new Handler<FailureDetectorPort.FailureDetectorEvent>() {

        @Override
        public void handle(FailureDetectorPort.FailureDetectorEvent event) {
            logger.debug("<{}/{}> received:{}", new Object[]{selfAddress.getId(), overlayId, event});
            for(VodAddress failedNode : event.getSuspectedNodes()) {
                view.clean(failedNode);
            }
        }
    };

    /**
     * Samples from Croupier used for bootstrapping gradient as well as faster
     * convergence(random samples)
     */
    Handler<CroupierSample> handleCroupierSample = new Handler<CroupierSample>() {
        @Override
        public void handle(CroupierSample sample) {
            logger.debug("<{}/{}> Croupier sample", selfAddress.getId(), overlayId);

            view.merge(sample.privateSample, selfCPV);
            view.merge(sample.publicSample, selfCPV);
        }
    };

    /**
     * Periodic Gradient Shuffle
     */
    Handler<ShuffleTimeout> handleShuffle = new Handler<ShuffleTimeout>() {
        @Override
        public void handle(ShuffleTimeout event) {
            logger.debug("<{}/{}> periodic shuffle", selfAddress.getId(), overlayId);
            if (outstandingShuffle != null) {
                logger.debug("<{}/{}> previous shuffle did not complete (timed out)", selfAddress.getId(), overlayId);
                view.clean(outstandingShuffle.getVodDestination());
                trigger(new FailureDetectorPort.FailureDetectorEvent(outstandingShuffle.getVodDestination()), failurePropagationPort);
            }
            if (view.isEmpty() || selfCPV == null) {
                logger.info("<{}/{}> view is empty or no self view - can't shuffle or publish view ", selfAddress.getId(), overlayId);
                return;
            }

            Collection<CroupierPeerView> gradientView = view.getView();
            logger.debug("<{}/{}> publishing view:{}", new Object[]{selfAddress.getId(), overlayId, gradientView});
            trigger(new GradientSample(gradientView), gradientPort);
            
            CroupierPeerView partnerCPV = view.getShuffleNode(selfCPV);
            Collection<CroupierPeerView> exchangeCPV = view.getExchangeCPV(partnerCPV, config.shuffleLength);
            Shuffle shuffle = new Shuffle(selfCPV, exchangeCPV);
            outstandingShuffle = new ShuffleNet.Request(selfAddress, partnerCPV.src, UUID.randomUUID(), overlayId, shuffle);
            logger.debug("<{}/{}> sending:{}", new Object[]{selfAddress.getId(), overlayId, outstandingShuffle});
            trigger(outstandingShuffle, network);
            view.incrementAges();
        }
    };

    Handler<ShuffleNet.Request> handleShuffleRequest = new Handler<ShuffleNet.Request>() {
        @Override
        public void handle(ShuffleNet.Request req) {
            logger.debug("<{}/{}> received:{}", new Object[]{selfAddress.getId(), overlayId, req});
            
            Collection<CroupierPeerView> exchangeCPV = view.getExchangeCPV(req.content.selfCPV, config.shuffleLength);
            Shuffle shuffle = new Shuffle(selfCPV, exchangeCPV);
            ShuffleNet.Response resp = new ShuffleNet.Response(selfAddress, req.getVodSource(), UUID.randomUUID(), overlayId, shuffle);
            logger.debug("<{}/{}> sending:{}", new Object[]{selfAddress.getId(), overlayId, resp});
            trigger(outstandingShuffle, network);

            view.merge(req.content.exchangeNodes, selfCPV);
        }
    };

    Handler<ShuffleNet.Response> handleShuffleResponse = new Handler<ShuffleNet.Response>() {
        @Override
        public void handle(ShuffleNet.Response resp) {
            logger.debug("<{}/{}> received:{}", new Object[]{selfAddress.getId(), overlayId, resp});
            
            if(outstandingShuffle == null || !resp.id.equals(outstandingShuffle.id)) {
                logger.info("<{}/{}> unexpected response:{}", new Object[]{selfAddress.getId(), overlayId, resp});
                return;
            }
            outstandingShuffle = null;
            view.merge(resp.content.exchangeNodes, selfCPV);
        }
    };

    private class ShuffleTimeout extends Timeout {

        public ShuffleTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }

    public class GradientInit extends Init<Gradient> {

        public final VodAddress selfAddress;
        public final GradientConfig gradientConfig;
        public final int overlayId;
        public final Comparator<PeerView> utilityComparator;
        public final int seed;

        public GradientInit(VodAddress selfAddress, GradientConfig config, int overlayId, Comparator<PeerView> utilityComparator, int seed) {
            this.selfAddress = selfAddress;
            this.gradientConfig = config;
            this.overlayId = overlayId;
            this.utilityComparator = utilityComparator;
            this.seed = seed;
        }
    }
}
