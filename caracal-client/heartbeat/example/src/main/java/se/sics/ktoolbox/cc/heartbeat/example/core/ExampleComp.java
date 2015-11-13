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
package se.sics.ktoolbox.cc.heartbeat.example.core;

import com.google.common.io.BaseEncoding;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.op.CCSimpleReady;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.event.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;
import se.sics.ktoolbox.cc.heartbeat.event.status.CCHeartbeatReady;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.impl.SystemKCWrapper;
import se.sics.p2ptoolbox.util.status.Status;
import se.sics.p2ptoolbox.util.status.StatusPort;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ExampleComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ExampleComp.class);
    private final String logPrefix;

    private Positive othersStatus = requires(StatusPort.class);
    private Positive overlaySample = requires(CCHeartbeatPort.class);
    private Positive timer = requires(Timer.class);

    private final SystemKCWrapper systemConfig;
    private final byte[] overlay1;
    private final byte[] overlay2;

    public ExampleComp(ExampleInit init) {
        this.systemConfig = new SystemKCWrapper(init.configCore);
        this.logPrefix = "<nid:" + systemConfig.id + "> ";
        this.overlay1 = init.overlay1;
        this.overlay2 = init.overlay2;
        log.info("{}intiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleCCHeartbeatReady, othersStatus);
        subscribe(handleSampleTimeout, timer);
        subscribe(handleOverlaySample, overlaySample);
    }

    //**************************************************************************
    Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} waiting for CCReady...", logPrefix);
        }
    };
    Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };
    ClassMatchedHandler handleCCHeartbeatReady
            = new ClassMatchedHandler<CCHeartbeatReady, Status.Internal<CCHeartbeatReady>>() {

                @Override
                public void handle(CCHeartbeatReady status, Status.Internal<CCHeartbeatReady> container) {
                    log.info("{} received CCReady", logPrefix);
                    scheduleSample();
                    trigger(new CCHeartbeat.Start(overlay1), overlaySample);
                    trigger(new CCHeartbeat.Start(overlay2), overlaySample);
                }
            };
    //**************************************************************************
    Handler handleSampleTimeout = new Handler<SampleTimeout>() {
        @Override
        public void handle(SampleTimeout event) {
            trigger(new CCOverlaySample.Request(UUID.randomUUID(), overlay1), overlaySample);
            trigger(new CCOverlaySample.Request(UUID.randomUUID(), overlay2), overlaySample);
        }
    };

    Handler handleOverlaySample = new Handler<CCOverlaySample.Response>() {
        @Override
        public void handle(CCOverlaySample.Response event) {
            log.info("{} overlay:{}, sample:{}",
                    new Object[]{logPrefix, BaseEncoding.base16().encode(event.req.overlayId), event.overlaySample});
        }
    };

    private void scheduleSample() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
        SampleTimeout st = new SampleTimeout(spt);
        spt.setTimeoutEvent(st);
        trigger(spt, timer);
    }

    public static class ExampleInit extends Init<ExampleComp> {

        public final KConfigCore configCore;
        public final byte[] overlay1;
        public final byte[] overlay2;

        public ExampleInit(KConfigCore configCore, byte[] overlay1, byte[] overlay2) {
            this.configCore = configCore;
            this.overlay1 = overlay1;
            this.overlay2 = overlay2;
        }
    }

    public class SampleTimeout extends Timeout {

        public SampleTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "Sample_Timeout";
        }
    }
}
