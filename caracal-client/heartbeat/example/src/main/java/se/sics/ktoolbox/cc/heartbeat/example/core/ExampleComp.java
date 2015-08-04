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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.common.op.CCSimpleReady;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.msg.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.msg.CCOverlaySample;
import se.sics.p2ptoolbox.util.config.SystemConfig;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ExampleComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ExampleComp.class);
    private final String logPrefix;

    private Positive overlaySample = requires(CCHeartbeatPort.class);
    private Positive timer = requires(Timer.class);

    private final SystemConfig systemConfig;
    private final byte[] overlay1;
    private final byte[] overlay2;

    public ExampleComp(ExampleInit init) {
        this.systemConfig = init.systemConfig;
        this.logPrefix = systemConfig.self.toString();
        this.overlay1 = init.overlay1;
        this.overlay2 = init.overlay2;
        log.info("{} intiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleCCReady, overlaySample);
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
    Handler handleCCReady = new Handler<CCSimpleReady>() {
        @Override
        public void handle(CCSimpleReady event) {
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
            trigger(new CCOverlaySample.Request(overlay1), overlaySample);
            trigger(new CCOverlaySample.Request(overlay2), overlaySample);
        }
    };

    Handler handleOverlaySample = new Handler<CCOverlaySample.Response>() {
        @Override
        public void handle(CCOverlaySample.Response event) {
            log.info("{} overlay:{}, sample:{}", 
                    new Object[]{logPrefix, BaseEncoding.base16().encode(event.overlayId), event.overlaySample});
        }
    };

    private void scheduleSample() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
        SampleTimeout st = new SampleTimeout(spt);
        spt.setTimeoutEvent(st);
        trigger(spt, timer);
    }

    public static class ExampleInit extends Init<ExampleComp> {

        public final SystemConfig systemConfig;
        public final byte[] overlay1;
        public final byte[] overlay2;

        public ExampleInit(SystemConfig systemConfig, byte[] overlay1, byte[] overlay2) {
            this.systemConfig = systemConfig;
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
