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
package se.sics.kafka;

import java.io.EOFException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AvroParserTask implements Runnable {

    private final Schema schema;
    private final KafkaAppendI callback;
    private byte[] data;

    public AvroParserTask(KafkaAppendI callback, Schema schema, byte[] data) {
        this.callback = callback;
        this.schema = schema;
        this.data = data;
    }

    @Override
    public void run() {
        while (true) {
            try {
                GenericRecord record;
                int readPos = 0;
                try {
                    while (true) {
                        Pair<GenericRecord, byte[]> result = AvroParser.blobToAvro(schema, data);
                        callback.record(result.getValue0());
                        data = result.getValue1();
                    }
                } catch (EOFException ex) {
                    callback.end(data);
                    break;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
