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
package se.sics.ktoolbox.kafka;

import com.google.common.io.BaseEncoding;
import io.hops.kafkautil.HopsKafkaConsumer;
import io.hops.kafkautil.HopsKafkaProducer;
import io.hops.kafkautil.HopsKafkaUtil;
import io.hops.kafkautil.SchemaNotFoundException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.DatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KafkaHelper {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaHelper.class);

    public static HopsKafkaProducer getKafkaProducer(KafkaResource kafkaResource) {
        LOG.warn("do not start multiple kafka workers in parallel - risk of race condition (setup/getProducer/getConsumer");
        HopsKafkaUtil hopsKafkaUtil = HopsKafkaUtil.getInstance();
        int projectId = Integer.parseInt(kafkaResource.projectId);
        LOG.info("getting producer session:{}, project:{} topic:{} domain:{} broker:{} rest:{} key:{} trust:{}",
                new Object[]{kafkaResource.sessionId, projectId, kafkaResource.topicName, kafkaResource.domain, kafkaResource.brokerEndpoint, kafkaResource.restEndpoint,
                    kafkaResource.keyStore, kafkaResource.trustStore});
        hopsKafkaUtil.setup(kafkaResource.sessionId, projectId, kafkaResource.topicName, kafkaResource.domain, kafkaResource.brokerEndpoint, kafkaResource.restEndpoint,
                kafkaResource.keyStore, kafkaResource.trustStore);
        HopsKafkaProducer kp;
        try {
            kp = hopsKafkaUtil.getHopsKafkaProducer(kafkaResource.topicName);
            return kp;
        } catch (SchemaNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static HopsKafkaConsumer getKafkaConsumer(KafkaResource kafkaResource) {
        LOG.warn("do not start multiple kafka workers in parallel - risk of race condition (setup/getProducer/getConsumer");
        HopsKafkaUtil hopsKafkaUtil = HopsKafkaUtil.getInstance();
        int projectId = Integer.parseInt(kafkaResource.projectId);
        LOG.info("getting consumer session:{}, project:{} topic:{} domain:{} broker:{} rest:{} key:{} trust:{}",
                new Object[]{kafkaResource.sessionId, projectId, kafkaResource.topicName, kafkaResource.domain, kafkaResource.brokerEndpoint, kafkaResource.restEndpoint,
                    kafkaResource.keyStore, kafkaResource.trustStore});
        hopsKafkaUtil.setup(kafkaResource.sessionId, projectId, kafkaResource.topicName, kafkaResource.domain, kafkaResource.brokerEndpoint, kafkaResource.restEndpoint,
                kafkaResource.keyStore, kafkaResource.trustStore);
        HopsKafkaConsumer kc;
        try {
            kc = hopsKafkaUtil.getHopsKafkaConsumer(kafkaResource.topicName);
            return kc;
        } catch (SchemaNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Schema getKafkaSchema(KafkaResource kafkaResource) {
        HopsKafkaUtil hopsKafkaUtil = HopsKafkaUtil.getInstance();
        int projectId = Integer.parseInt(kafkaResource.projectId);
        LOG.info("getting schema session:{}, project:{} topic:{} domain:{} broker:{} rest:{} key:{} trust:{}",
                new Object[]{kafkaResource.sessionId, projectId, kafkaResource.topicName, kafkaResource.domain, kafkaResource.brokerEndpoint, kafkaResource.restEndpoint,
                    kafkaResource.keyStore, kafkaResource.trustStore});
        hopsKafkaUtil.setup(kafkaResource.sessionId, projectId, kafkaResource.topicName, kafkaResource.domain, kafkaResource.brokerEndpoint, kafkaResource.restEndpoint,
                kafkaResource.keyStore, kafkaResource.trustStore);
        String stringSchema;
        try {
            stringSchema = hopsKafkaUtil.getSchema();
        } catch (SchemaNotFoundException ex) {
             throw new RuntimeException(ex);
        }
        LOG.info("schema:{}", stringSchema);
        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(stringSchema);
        return schema;
    }

    public static byte[] getSimpleAvroMsgsAsBlob(Schema schema, int nrMsgs, Random rand) {
        ByteBuf buf = Unpooled.buffer();
        OutputStream out = new ByteBufOutputStream(buf);
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);
        try {
            dataFileWriter.create(schema, out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        GenericRecordBuilder grb;

        for (int i = 0; i < nrMsgs; i++) {
            grb = new GenericRecordBuilder(schema);
            for (Field field : schema.getFields()) {
                //TODO Alex - I assume each field is a string
                grb.set(field, "val" + (1000+rand.nextInt(1000)));
            }
            try {
                dataFileWriter.append(grb.build());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        byte[] result = new byte[buf.writerIndex()];
        buf.readBytes(result);
        LOG.info("avro to blob:{}", BaseEncoding.base16().encode(result));
        return result;
    }
}
