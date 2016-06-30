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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AvroParser {

    public static Pair<GenericRecord, byte[]> blobToAvro(Schema schema, byte[] data) throws EOFException {
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        GenericRecord record;
        int rest;
        try {
            record = reader.read(null, decoder);
            int from = data.length - decoder.inputStream().available();
            int to = data.length;
            byte[] leftover = Arrays.copyOfRange(data, from, to);
            return Pair.with(record, leftover);
        } catch (EOFException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<GenericRecord> blobToAvroList(Schema schema, InputStream in) {
        List<GenericRecord> records = new ArrayList<>();
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
        try {
            while (true) {
                GenericRecord record = reader.read(null, decoder);
                records.add(record);
            }
        } catch (EOFException ex) {

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return records;
    }

    public static byte[] avroToBlob(Schema schema, GenericRecord record) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        try {
            writer.write(record, encoder);
            encoder.flush();
        } catch (Exception ex) {
            throw new RuntimeException("hmmm", ex);
        }
        return out.toByteArray();
    }
}
