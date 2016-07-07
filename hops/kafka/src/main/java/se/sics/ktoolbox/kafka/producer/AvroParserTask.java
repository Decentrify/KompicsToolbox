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
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AvroParserTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AvroParserTask.class);

    private final AvroMsgProducer output;
    private final ParserMngrI callback;
    private final ByteBuf data;

    public AvroParserTask(ParserMngrI callback, AvroMsgProducer out, ByteBuf data) {
        this.callback = callback;
        this.output = out;
        this.data = data;
    }

    @Override
    public void run() {
        Schema schema = output.getSchema();
        int producedMsgs = 0;
        while (true) {
            GenericRecord record = AvroParser.blobToAvro(schema, data);
            if (record != null) {
                producedMsgs++;
                output.append(record);
            } else {
                int leftoverSize = data.writerIndex() - data.readerIndex();
                LOG.info("leftover:{}", leftoverSize);
                byte[] leftover = new byte[leftoverSize];
                data.readBytes(leftover);
                callback.end(producedMsgs, leftover);
                break;
            }
        }
    }
}
