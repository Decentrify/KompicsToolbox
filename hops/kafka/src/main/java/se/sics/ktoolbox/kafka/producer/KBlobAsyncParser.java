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
package se.sics.ktoolbox.kafka.producer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.kafka.KafkaResource;
import se.sics.ktoolbox.util.BKOutputStream;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KBlobAsyncParser implements ParserMngrI, BKOutputStream {
    private static final Logger LOG = LoggerFactory.getLogger(KBlobAsyncParser.class);

    private final ExecutorService avroParser = Executors.newFixedThreadPool(1);
    private final AvroMsgProducer output;
    private long producedMsgs = 0;
    //*****************************SYNCHRONIZED*********************************
    private ByteBuf buf;
    private byte[] leftover; //also acts as a flag for ongoing task

    private KBlobAsyncParser(AvroMsgProducer output) {
        this.output = output;
        this.leftover = new byte[0];
    }
    
    public KBlobAsyncParser(KafkaResource resource) {
        this(new KafkaProducer(resource));
    }
    
    //For junit testing purposes
    KBlobAsyncParser(Schema schema) {
        this(new LogProducer(schema));
    }
    

    //***************************PARSER_MNGR_I*********************************
    @Override
    public synchronized void end(int producerdMsgs, byte[] leftover) {
        this.leftover = leftover;
        this.producedMsgs += producerdMsgs;
        if (buf != null) {
            newParseTask();
        }
    }
    //****************************K_STREAM_OUT**********************************
    @Override
    public synchronized void write(byte[] data) {
        if (buf == null) {
            buf = Unpooled.buffer();
        }
        buf.writeBytes(data);
        if (leftover != null) {
            newParseTask();
        }
    }

    @Override
    public synchronized boolean isIdle() {
        return buf == null && leftover != null; 
    }
    
    @Override
    public void terminate() {
        boolean term;
        try {
            avroParser.shutdownNow();
            term = avroParser.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (!term) {
            throw new RuntimeException("cannot kill threads");
        }
    }

    //**************************************************************************
    public long producedMsgs() {
        return producedMsgs;
    }
    
    private void newParseTask() {
        int dataLength = leftover.length + buf.writerIndex();
        ByteBuf newData = Unpooled.buffer(dataLength, dataLength);
        newData.writeBytes(leftover);
        newData.writeBytes(buf, buf.writerIndex());
        if (newData.writableBytes() != 0) {
            throw new RuntimeException("logic error - writable bytes:" + newData.writableBytes() + " expected:0");
        }
        avroParser.execute(new AvroParserTask(this, output, newData));
        buf = null;
        leftover = null;
    }
}
