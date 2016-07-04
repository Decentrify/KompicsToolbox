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
package se.sics.ktoolbox.kafka.parser;

import io.hops.kafkautil.HopsKafkaProducer;
import io.hops.kafkautil.HopsKafkaUtil;
import io.hops.kafkautil.SchemaNotFoundException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import se.sics.ktoolbox.kafka.KafkaResource;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KafkaProducer implements AvroMsgProducer {
    private final HopsKafkaUtil hopsKafkaUtil;
    private final HopsKafkaProducer producer;

    public KafkaProducer(KafkaResource resource) {
        hopsKafkaUtil = HopsKafkaUtil.getInstance();
        int projectId = Integer.parseInt(resource.projectId);
        hopsKafkaUtil.setup(resource.sessionId, projectId, resource.topicName, resource.domain, resource.brokerEndpoint, resource.restEndpoint, 
                resource.keyStore, resource.trustStore);
        try {
            this.producer = hopsKafkaUtil.getHopsKafkaProducer(resource.topicName);
        } catch (SchemaNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public void append(GenericRecord record) {
        producer.produce(record);
    }

    @Override
    public Schema getSchema() {
        return producer.getSchema();
    }
}
