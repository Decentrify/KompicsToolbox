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
package se.sics.ktoolbox.util.profiling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 * Use this class within the same thread - start/mid/end should all be called on
 * the same thread. You can still call multiple sessions between threads
 */
public class KProfiler {

    private final static Logger LOG = LoggerFactory.getLogger(KProfiler.class);

    private final Type type;
    private Session session;

    public KProfiler(Type type) {
        this.type = type;
    }

    public void start(String component, String handler) {
        assert session == null;
        session = protoFactory(component, handler);
    }

    public void next(String midPoint) {
        assert session != null;
        session.next(midPoint);
    }

    public void end() {
        assert session != null;
        session.end();
        session = null;
    }

    private Session protoFactory(String component, String handler) {
        switch (type) {
            case LOG:
                return new LogSession(component, handler);
            case NONE:
                return new NopSession();
            default:
                throw new RuntimeException("logic error - unhandled type:" + type);
        }
    }

    public static enum Type {

        NONE, LOG;
    }

    public static interface Session {

        public void next(String midPoint);

        public void end();
    }

    private static class NopSession implements Session {

        @Override
        public void next(String midPoint) {
        }

        @Override
        public void end() {
        }
    }

    private static class LogSession implements Session {

        public final Long threadId;
        public final String component;
        public final String handler;

        public final Long startTime;
        public Long intermediateTime;
        public Long endTime;

        public LogSession(String component, String handler) {
            this.threadId = Thread.currentThread().getId();
            this.component = component;
            this.handler = handler;
            this.startTime = System.nanoTime();
            this.intermediateTime = startTime;
        }

        @Override
        public void next(String midPoint) {
            assert Thread.currentThread().getId() == threadId;

            long aux = intermediateTime;
            intermediateTime = System.nanoTime();
            LOG.info("{} {} {} micros:{}", new Object[]{component, handler, midPoint, (intermediateTime - aux)/1000});
        }

        @Override
        public void end() {
            assert Thread.currentThread().getId() == threadId;

            endTime = System.nanoTime();
            LOG.info("{} {} mid micros:{}", new Object[]{component, handler, (endTime - intermediateTime)/1000});
            LOG.info("{} {} total micros:{}", new Object[]{component, handler, (endTime - startTime)/1000});
        }
    }
}
