/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.p2ptoolbox.chunkmanager;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ChannelFilter;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.chunkmanager.util.Chunk;
import se.sics.p2ptoolbox.chunkmanager.util.ChunkPrefixHelper;
import se.sics.p2ptoolbox.chunkmanager.util.IncompleteChunkTracker;
import se.sics.p2ptoolbox.chunkmanager.util.CompleteChunkTracker;
import se.sics.p2ptoolbox.chunkmanager.util.FragmentableTrafficFilter;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.filters.AndFilter;
import se.sics.p2ptoolbox.util.filters.NotFilter;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;

/**
 * Created by alidar on 10/21/14.
 */
public class ChunkManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ChunkManagerComp.class);

    private Positive<Network> requiredNetwork = requires(Network.class);
    private Negative<Network> providedNetwork = provides(Network.class);
    private Positive<Timer> timer = positive(Timer.class);

    private final SystemConfig systemConfig;
    private final ChunkManagerConfig config;
    private final String logPrefix;
    private final ChannelFilter<Msg, Boolean> handleTraffic = new FragmentableTrafficFilter();

    private final Map<UUID, Pair<IncompleteChunkTracker, UUID>> incomingChunks;

    public ChunkManagerComp(CMInit init) {
        this.systemConfig = init.systemConfig;
        this.config = init.config;
        this.logPrefix = systemConfig.self.toString();
        log.info("{} initiating...", logPrefix);

        this.incomingChunks = new HashMap<UUID, Pair<IncompleteChunkTracker, UUID>>();

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleOutgoing, providedNetwork);
        subscribe(handleIncoming, requiredNetwork);
        subscribe(handleCleanupTimeout, timer);
    }

    //**************************************************************************
    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
        }
    };
    private Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };
    //**************************************************************************

    Handler handleOutgoing = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            log.trace("{} received:{}", logPrefix, msg);
            if (!handleTraffic.getValue(msg)) {
                log.debug("{} forwarding outgoing non fragmentable message:{}", logPrefix, msg);
                trigger(msg, requiredNetwork);
                return;
            }
            BasicContentMsg contentMsg = (BasicContentMsg)msg;

            ByteBuf content = Unpooled.buffer();
            ByteBuf header = Unpooled.buffer();
            Serializers.toBinary(contentMsg.getContent(), content);
            Serializers.toBinary(contentMsg.getHeader(), header);

            //we have to accommodate the headers info of the chunked message as well.
            int headerSize = header.readableBytes();
            //TODO Alex - make Chunked a trait - hardcoded extra size for fields in chunk
            int datagramContentSize = config.datagramUsableSize - headerSize - ChunkPrefixHelper.getChunkPrefixSize();
            if (datagramContentSize <= 0) {
                log.error("{} chunk manager is badly configured", logPrefix);
                throw new RuntimeException("chunk manager is badly configured");
            }
            if (content.readableBytes() < datagramContentSize) {
                log.debug("{} forwarding UDP small message:{}", logPrefix, msg);
                trigger(msg, requiredNetwork);
                return;
            }

            CompleteChunkTracker cct = new CompleteChunkTracker(UUID.randomUUID(), content, datagramContentSize);
            for (Chunk chunk : cct.chunks.values()) {
                BasicContentMsg chunkMsg = new BasicContentMsg(msg.getHeader(), chunk);
                log.debug("{} sending chunk nr:{}", logPrefix, chunk.chunkNr);
                trigger(chunkMsg, requiredNetwork);
            }
        }
    };

    Handler handleIncoming = new Handler<BasicContentMsg>() {
        @Override
        public void handle(BasicContentMsg msg) {
            log.trace("{} received:{}", logPrefix, msg);
            if (!(msg.getContent() instanceof Chunk)) {
                log.debug("{} forwarding incoming non fragmentable message:{}", logPrefix, msg);
                trigger(msg, providedNetwork);
                return;
            }

            Chunk chunk = (Chunk) msg.getContent();

            if (!incomingChunks.containsKey(chunk.messageId)) {
                IncompleteChunkTracker chunkTracker = new IncompleteChunkTracker(chunk.lastChunk);
                UUID cleanupTimeout = scheduleCleanupTimeout(chunk.messageId);
                incomingChunks.put(chunk.messageId, Pair.with(chunkTracker, cleanupTimeout));
            }

            log.debug("{} received chunk:{} for message:{}", new Object[]{logPrefix, chunk.chunkNr, chunk.messageId});

            IncompleteChunkTracker chunkTracker = incomingChunks.get(chunk.messageId).getValue0();
            chunkTracker.add(chunk);
            if (chunkTracker.isComplete()) {
                cancelCleanupTimeout(incomingChunks.get(chunk.messageId).getValue1());
                incomingChunks.remove(chunk.messageId);
                byte[] msgBytes = chunkTracker.getMsg();

                Header header = msg.getHeader();
                Object content = Serializers.fromBinary(Unpooled.wrappedBuffer(msgBytes), Optional.absent());
                BasicContentMsg rebuiltMsg = new BasicContentMsg(header, content);
                log.debug("{} rebuilt chunked message:{}", logPrefix, rebuiltMsg);
                trigger(rebuiltMsg, providedNetwork);
            }
        }
    };

    final Handler handleCleanupTimeout = new Handler<CleanupTimeout>() {
        @Override
        public void handle(CleanupTimeout timeout) {
            if (incomingChunks.remove(timeout.messageId) == null) {
                log.debug("{} chunked message:{} timed out", logPrefix, timeout.messageId);
            }
        }
    };

    private UUID scheduleCleanupTimeout(UUID messageId) {
        ScheduleTimeout spt = new ScheduleTimeout(config.cleanupTimeout);
        CleanupTimeout ct = new CleanupTimeout(spt, messageId);
        spt.setTimeoutEvent(ct);
        trigger(spt, timer);
        return ct.getTimeoutId();
    }

    private void cancelCleanupTimeout(UUID timeoutId) {
        CancelTimeout cpt = new CancelTimeout(timeoutId);
        trigger(cpt, timer);
    }

    public static class CMInit extends Init<ChunkManagerComp> {

        public final SystemConfig systemConfig;
        public final ChunkManagerConfig config;

        public CMInit(SystemConfig systemConfig, ChunkManagerConfig config) {
            this.systemConfig = systemConfig;
            this.config = config;
        }
    }

    public static class CleanupTimeout extends Timeout {

        public final UUID messageId;

        public CleanupTimeout(ScheduleTimeout st, UUID messageId) {
            super(st);
            this.messageId = messageId;
        }

        @Override
        public String toString() {
            return "CLEANUP_TIMEOUT";
        }
    }
}
