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
package se.sics.ktoolbox.netmngr.chunk;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.netmngr.chunk.util.ChunkPrefixHelper;
import se.sics.ktoolbox.netmngr.chunk.util.CompleteChunkTracker;
import se.sics.ktoolbox.netmngr.chunk.util.IncompleteChunkTracker;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.other.Chunkable;

/**
 * Created by alidar on 10/21/14.
 */
public class ChunkMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkMngrComp.class);
    private final String logPrefix;

    //*****************************CONNECTIONS**********************************
    private final Positive<Network> requiredNetwork = requires(Network.class);
    private final Negative<Network> providedNetwork = provides(Network.class);
    private final Positive<Timer> timer = requires(Timer.class);
    //****************************CONFIGURATION*********************************
    private final SystemKCWrapper systemConfig;
    private final ChunkMngrKCWrapper chunkMngrConfig;
    //*******************************STATE**************************************
    private final Map<Identifier, Pair<CompleteChunkTracker, UUID>> outgoingChunks = new HashMap<>();
    private final Map<Identifier, Pair<IncompleteChunkTracker, UUID>> incomingChunks = new HashMap<>();
    private final IdentifierFactory eventIds;
    
    public ChunkMngrComp(Init init) {
        this.systemConfig = new SystemKCWrapper(config());
        this.chunkMngrConfig = new ChunkMngrKCWrapper(config());
        this.logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initiating...", logPrefix);
        
        this.eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, 
          java.util.Optional.of(systemConfig.seed));
        subscribe(handleStart, control);
        subscribe(handleOutgoing, providedNetwork);
        subscribe(handleOutgoing, requiredNetwork);
        subscribe(handleIncoming, requiredNetwork);
        subscribe(handleCleanupTimeout, timer);
    }

    //*********************************CONTROL**********************************
    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };
    //**************************************************************************
    ClassMatchedHandler handleOutgoing
            = new ClassMatchedHandler<Chunkable, BasicContentMsg<?, ?, Chunkable>>() {

                @Override
                public void handle(Chunkable content, BasicContentMsg<?, ?, Chunkable> container) {
                    LOG.trace("{}received outgoing:{}", logPrefix, container);

                    ByteBuf contentBytes = Unpooled.buffer();
                    ByteBuf headerBytes = Unpooled.buffer();
                    Serializers.toBinary(content, contentBytes);
                    Serializers.toBinary(container.getHeader(), headerBytes);

                    //we have to accommodate the headers info of the chunked message as well.
                    int headerSize = headerBytes.readableBytes();
                    //TODO Alex - hardcoded extra size for fields in chunk
                    int datagramContentSize = chunkMngrConfig.datagramUsableSize - headerSize - ChunkPrefixHelper.getChunkPrefixSize();
                    if (datagramContentSize <= 0) {
                        LOG.error("{}chunk manager is badly configured", logPrefix);
                        throw new RuntimeException("chunk manager is badly configured");
                    }
                    if (contentBytes.readableBytes() < datagramContentSize) {
                        LOG.trace("{}forwarding to UDP - small message:{}", logPrefix, container);
                        trigger(container, requiredNetwork);
                        return;
                    }

                    Identifier originId;
                    if(content instanceof Identifiable) {
                        originId = ((Identifiable)content).getId();
                    } else {
                        originId = eventIds.randomId();
                    }
                    CompleteChunkTracker cct = new CompleteChunkTracker(originId, contentBytes, datagramContentSize);
                    for (Chunk chunk : cct.chunks.values()) {
                        KContentMsg chunkMsg = new BasicContentMsg(container.getHeader(), chunk);
                        LOG.trace("{}sending chunk nr:{}", logPrefix, chunk.chunkNr);
                        trigger(chunkMsg, requiredNetwork);
                    }
                    UUID cleanupTimeout = scheduleCleanupTimeout(originId);
                    outgoingChunks.put(originId, Pair.with(cct, cleanupTimeout));
                }
            };

    ClassMatchedHandler handleIncoming
            = new ClassMatchedHandler<Chunk, KContentMsg<KAddress, KHeader<KAddress>, Chunk>>() {

                @Override
                public void handle(Chunk chunk, KContentMsg<KAddress, KHeader<KAddress>, Chunk> container) {
                    LOG.trace("{}received incoming:{}", logPrefix, container);
                    Pair<IncompleteChunkTracker, UUID> chunkTrackerPair = incomingChunks.get(chunk.originId);
                    if (chunkTrackerPair == null) {
                        IncompleteChunkTracker chunkTracker = new IncompleteChunkTracker(chunk.lastChunk);
                        UUID cleanupTimeout = scheduleCleanupTimeout(chunk.originId);
                        chunkTrackerPair = Pair.with(chunkTracker, cleanupTimeout);
                        incomingChunks.put(chunk.originId, chunkTrackerPair);
                    }
                    
                    chunkTrackerPair.getValue0().add(chunk);
                    if (chunkTrackerPair.getValue0().isComplete()) {
                        cancelCleanupTimeout(incomingChunks.get(chunk.originId).getValue1());
                        incomingChunks.remove(chunk.originId);
                        byte[] msgBytes = chunkTrackerPair.getValue0().getMsg();

                        KHeader header = container.getHeader();
                        Object content = Serializers.fromBinary(Unpooled.wrappedBuffer(msgBytes), Optional.absent());
                        BasicContentMsg rebuiltMsg = new BasicContentMsg(header, content);
                        LOG.debug("{}rebuilt chunked message:{}", logPrefix, rebuiltMsg);
                        trigger(rebuiltMsg, providedNetwork);
                    }
                }
            };

    final Handler handleCleanupTimeout = new Handler<CleanupTrackerTimeout>() {
        @Override
        public void handle(CleanupTrackerTimeout timeout) {
            if (incomingChunks.remove(timeout.originId) != null) {
                LOG.debug("{}incoming chunked message:{} timed out", logPrefix, timeout.originId);
            }
            if (outgoingChunks.remove(timeout.originId) != null) {
                LOG.debug("{}outgoing chunked message:{} timed out", logPrefix, timeout.originId);
            }
            LOG.debug("{}late timeout for chunk message:{}", logPrefix, timeout.originId);
        }
    };

    private UUID scheduleCleanupTimeout(Identifier originId) {
        ScheduleTimeout spt = new ScheduleTimeout(chunkMngrConfig.cleanupTimeout);
        CleanupTrackerTimeout ct = new CleanupTrackerTimeout(spt, originId);
        spt.setTimeoutEvent(ct);
        trigger(spt, timer);
        return ct.getTimeoutId();
    }

    private void cancelCleanupTimeout(UUID timeoutId) {
        CancelTimeout cpt = new CancelTimeout(timeoutId);
        trigger(cpt, timer);
    }

    public static class Init extends se.sics.kompics.Init<ChunkMngrComp> {

        public Init() {
        }
    }

    public static class CleanupTrackerTimeout extends Timeout {

        public final Identifier originId;

        public CleanupTrackerTimeout(ScheduleTimeout st, Identifier originId) {
            super(st);
            this.originId = originId;
        }

        @Override
        public String toString() {
            return "ChunkMngr_CleanupTracker<" + getTimeoutId()+ ">";
        }
    }
}
