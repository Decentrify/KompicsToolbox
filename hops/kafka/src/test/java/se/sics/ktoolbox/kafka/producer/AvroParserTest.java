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

import com.google.common.io.BaseEncoding;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.BKOutputStream;
import se.sics.ktoolbox.util.RABKOuputStreamImpl;
import se.sics.ktoolbox.util.RABKOutputStream;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AvroParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(AvroParserTest.class);
    private String logPrefix;

    @Test
    public void testStreamingPartialRecords() throws IOException, InterruptedException {
        LOG.info("***********************************************************");
        LOG.info("streaming partial records test");
        Schema schema = SchemaBuilder
                .record("schema")
                .namespace("org.apache.avro.ipc")
                .fields()
                .name("field1").type().nullable().stringType().noDefault()
                .name("field2").type().nullable().stringType().noDefault()
                .name("field3").type().nullable().stringType().noDefault()
                .endRecord();
        KBlobAsyncParser kso = new KBlobAsyncParser(schema);

        GenericRecordBuilder recordBuilder = new GenericRecordBuilder(schema);
        byte[] bRecord, piece;
        GenericData.Record record;
        GenericRecord pipedRecord;

        int nrRecords = 7;
        int recordSize = 18;
        int bufferSize = nrRecords * recordSize;
        ByteBuffer bb = ByteBuffer.allocate(nrRecords * recordSize);
        for (int i = 0; i < nrRecords; i++) {
            recordBuilder.set("field1", "val" + i);
            recordBuilder.set("field2", "val" + i);
            recordBuilder.set("field3", "val" + i);
            record = recordBuilder.build();

            bRecord = AvroParser.avroToBlob(schema, record);
            bb.put(bRecord);
        }
        Assert.assertEquals(nrRecords * recordSize, bb.position());
        bb.position(0);

        piece = new byte[18];
        bb.get(piece);
        LOG.info("writting piece 0");
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting part of piece 1");
        piece = new byte[10];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting rest of piece 1");
        piece = new byte[8];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting piece 2,3");
        piece = new byte[36];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting piece 4 and part of piece 5");
        piece = new byte[27];
        bb.get(piece);
        kso.write(piece);

        Thread.sleep(1000);
        LOG.info("writting rest of piece 5 and piece 6");
        piece = new byte[27];
        bb.get(piece);
        kso.write(piece);

        while (true) {
            if (kso.isIdle()) {
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertEquals(nrRecords, kso.producedMsgs());
    }

    @Test
    public void testRandomAccessStreamingPartialRecords() throws IOException, InterruptedException {
        LOG.info("***********************************************************");
        LOG.info("streaming out of order partial records test");
        Schema schema = SchemaBuilder
                .record("schema")
                .namespace("org.apache.avro.ipc")
                .fields()
                .name("field1").type().nullable().stringType().noDefault()
                .name("field2").type().nullable().stringType().noDefault()
                .name("field3").type().nullable().stringType().noDefault()
                .endRecord();
        KBlobAsyncParser kso = new KBlobAsyncParser(schema);
        List<BKOutputStream> outStreams = new ArrayList<>();
        outStreams.add(kso);
        RABKOutputStream out = new RABKOuputStreamImpl(outStreams, 0);

        GenericRecordBuilder recordBuilder = new GenericRecordBuilder(schema);
        byte[] bRecord, piece;
        GenericData.Record record;
        GenericRecord pipedRecord;
        ByteBuf aux;
        int pos;

        int nrRecords = 7;
        int recordSize = 18;
        int bufferSize = nrRecords * recordSize;
        ByteBuf bb = Unpooled.buffer(bufferSize, bufferSize);
        for (int i = 0; i < nrRecords; i++) {
            recordBuilder.set("field1", "val" + i);
            recordBuilder.set("field2", "val" + i);
            recordBuilder.set("field3", "val" + i);
            record = recordBuilder.build();

            bRecord = AvroParser.avroToBlob(schema, record);
            bb.writeBytes(bRecord);
        }
        Assert.assertEquals(nrRecords * recordSize, bb.writerIndex());

        piece = new byte[18];
        pos = 0 * recordSize;
        bb.readerIndex(pos);
        bb.readBytes(piece);
        LOG.info("writting piece 0");
        out.write(pos, piece);

        Thread.sleep(1000);
        LOG.info("writting piece 2,3");
        piece = new byte[36];
        pos = 2 * recordSize;
        bb.readerIndex(pos);
        bb.readBytes(piece);
        out.write(pos, piece);

        Thread.sleep(1000);
        LOG.info("writting part of piece 1");
        piece = new byte[10];
        pos = 1 * recordSize;
        bb.readerIndex(pos);
        bb.readBytes(piece);
        out.write(pos, piece);

        Thread.sleep(1000);
        LOG.info("writting rest of piece 1");
        piece = new byte[8];
        pos = 1 * recordSize + 10;
        bb.readerIndex(pos);
        bb.readBytes(piece);
        out.write(pos, piece);

        Thread.sleep(1000);
        LOG.info("writting rest of piece 5 and piece 6");
        piece = new byte[27];
        pos = 5 * recordSize + 9;
        bb.readerIndex(pos);
        bb.readBytes(piece);
        out.write(pos, piece);

        Thread.sleep(1000);
        LOG.info("writting piece 4 and part of piece 5");
        piece = new byte[27];
        pos = 4 * recordSize;
        bb.readerIndex(pos);
        bb.readBytes(piece);
        out.write(pos, piece);

        while (true) {
            if (kso.isIdle()) {
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertEquals(nrRecords, kso.producedMsgs());
    }

    @Test
    public void test1000() throws InterruptedException {
        LOG.info("***********************************************************");
        LOG.info("streaming out of order partial records test");
        Schema schema = SchemaBuilder
                .record("schema")
                .namespace("org.apache.avro.ipc")
                .fields()
                .name("field1").type().nullable().stringType().noDefault()
                .name("field2").type().nullable().stringType().noDefault()
                .name("field3").type().nullable().stringType().noDefault()
                .endRecord();

        Random rand = new Random(1234);
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 10; i++) {
            buf.writeBytes(AvroParser.nAvroToBlob(schema, 100, rand));
        }

        KBlobAsyncParser kso = new KBlobAsyncParser(schema);
        List<BKOutputStream> outStreams = new ArrayList<>();
        outStreams.add(kso);
        RABKOutputStream out = new RABKOuputStreamImpl(outStreams, 0);

        byte[] piece = new byte[buf.writerIndex()];
        buf.readBytes(piece);
        LOG.info("piece:{}", BaseEncoding.base16().encode(piece));
        out.write(piece);

        while (true) {
            if (kso.isIdle()) {
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertEquals(1000, kso.producedMsgs());
    }

    @Test
    public void test1MilTime() throws InterruptedException {
        LOG.info("***********************************************************");
        LOG.info("streaming out of order partial records test");
        Schema schema = SchemaBuilder
                .record("schema")
                .namespace("org.apache.avro.ipc")
                .fields()
                .name("field1").type().nullable().stringType().noDefault()
                .name("field2").type().nullable().stringType().noDefault()
                .name("field3").type().nullable().stringType().noDefault()
                .endRecord();

        Random rand = new Random(1234);
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 2000; i++) {
            buf.writeBytes(AvroParser.nAvroToBlob(schema, 1000, rand));
        }

        KBlobAsyncParser kso = new KBlobAsyncParser(schema);
        List<BKOutputStream> outStreams = new ArrayList<>();
        outStreams.add(kso);
        RABKOutputStream out = new RABKOuputStreamImpl(outStreams, 0);

        byte[] piece = new byte[buf.writerIndex()];
        buf.readBytes(piece);
        long startTime = System.currentTimeMillis();
        out.write(piece);

        while (true) {
            if (kso.isIdle()) {
                break;
            }
            Thread.sleep(1000);
        }
        long endTime = System.currentTimeMillis();
        LOG.info("spent time(ms):{}", (endTime - startTime));
        Assert.assertEquals(1000, kso.producedMsgs());
    }
}
