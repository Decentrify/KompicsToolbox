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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AvroParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(AvroParserTest.class);
    private String logPrefix;

//    @Test
    public void testOneFullRecord() throws IOException {
        InputStream in;

        Schema schema = SchemaBuilder
                .record("schema")
                .namespace("org.apache.avro.ipc")
                .fields()
                .name("field1").type().nullable().stringType().noDefault()
                .name("field2").type().nullable().stringType().noDefault()
                .name("field3").type().nullable().stringType().noDefault()
                .endRecord();

        GenericRecordBuilder recordBuilder = new GenericRecordBuilder(schema);
        byte[] bRecord;
        GenericData.Record record;
        List<GenericRecord> pipedRecords;

        //
        recordBuilder.set("field1", "val1");
        recordBuilder.set("field2", "val1");
        recordBuilder.set("field3", "val1");
        record = recordBuilder.build();

        bRecord = AvroParser.avroToBlob(schema, record);
        Assert.assertEquals(18, bRecord.length);

        in = new ByteArrayInputStream(bRecord);
        pipedRecords = AvroParser.blobToAvroList(schema, in);

        Assert.assertEquals(0, in.available());
        Assert.assertEquals(1, pipedRecords.size());
        Assert.assertEquals(record, pipedRecords.get(0));
    }

//    @Test
    public void testNFullRecords() throws IOException {
        Schema schema = SchemaBuilder
                .record("schema")
                .namespace("org.apache.avro.ipc")
                .fields()
                .name("field1").type().nullable().stringType().noDefault()
                .name("field2").type().nullable().stringType().noDefault()
                .name("field3").type().nullable().stringType().noDefault()
                .endRecord();

        GenericRecordBuilder recordBuilder = new GenericRecordBuilder(schema);
        byte[] bRecord;
        GenericData.Record record;
        List<GenericRecord> pipedRecords;
        ByteBuffer bb = ByteBuffer.allocate(54);

        int nrRecords = 3;
        int recordSize = 18;
        for (int i = 0; i < nrRecords; i++) {
            recordBuilder.set("field1", "val1");
            recordBuilder.set("field2", "val1");
            recordBuilder.set("field3", "val1");
            record = recordBuilder.build();

            bRecord = AvroParser.avroToBlob(schema, record);
            bb.put(bRecord);
        }
        Assert.assertEquals(nrRecords * recordSize, bb.position());

        InputStream in;
        in = new ByteArrayInputStream(bb.array());
        pipedRecords = AvroParser.blobToAvroList(schema, in);

        Assert.assertEquals(0, in.available());
        Assert.assertEquals(3, pipedRecords.size());
    }

    @Test
    public void testStreamingPartialRecords() throws IOException, InterruptedException {
        Schema schema = SchemaBuilder
                .record("schema")
                .namespace("org.apache.avro.ipc")
                .fields()
                .name("field1").type().nullable().stringType().noDefault()
                .name("field2").type().nullable().stringType().noDefault()
                .name("field3").type().nullable().stringType().noDefault()
                .endRecord();
        KafkaProducer kso = new KafkaProducer(schema);

        GenericRecordBuilder recordBuilder = new GenericRecordBuilder(schema);
        byte[] bRecord, piece;
        GenericData.Record record;
        GenericRecord pipedRecord;

        int nrRecords = 7;
        int recordSize = 18;
        ByteBuffer bb = ByteBuffer.allocate(nrRecords * recordSize);
        for (int i = 0; i < nrRecords; i++) {
            recordBuilder.set("field1", "val1");
            recordBuilder.set("field2", "val1");
            recordBuilder.set("field3", "val1");
            record = recordBuilder.build();

            bRecord = AvroParser.avroToBlob(schema, record);
            bb.put(bRecord);
        }
        Assert.assertEquals(nrRecords * recordSize, bb.position());
        bb.position(0);

        piece = new byte[18];
        bb.get(piece);
        LOG.info("writting piece 1");
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting part of piece 2");
        piece = new byte[10];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting rest of piece 2");
        piece = new byte[8];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting piece 3,4");
        piece = new byte[36];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting piece 5 and part of piece 6");
        piece = new byte[27];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting rest of piece 6 and piece 7");
        piece = new byte[27];
        bb.get(piece);
        kso.write(piece);

        while (true) {
            if (kso.isIdle()) {
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertEquals(nrRecords, kso.kafkaMsgs);
    }
}
