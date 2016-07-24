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

import se.sics.ktoolbox.kafka.avro.AvroMsgProducer;
import se.sics.ktoolbox.kafka.avro.KafkaProducer;
import se.sics.ktoolbox.util.stream.StreamResource;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KafkaResource implements StreamResource {
    public final String brokerEndpoint;
    public final String restEndpoint;
    public final String domain;
    public final String sessionId;
    public final String projectId;
    public final String topicName;
    public final String keyStore;
    public final String trustStore;

    public KafkaResource(String brokerEndpoint, String restEndpoint, String domain, String sessionId, 
            String projectId, String topicName, String keyStore, String trustStore) {
        this.brokerEndpoint = brokerEndpoint;
        this.restEndpoint = restEndpoint;
        this.domain = domain;
        this.sessionId = sessionId;
        this.projectId = projectId;
        this.topicName = topicName;
        this.keyStore = keyStore;
        this.trustStore = trustStore;
    }

    @Override
    public Class<KafkaPort> resourcePort() {
        return KafkaPort.class;
    }

    @Override
    public String getResourceName() {
        return "kafka - topic:" + topicName; 
    }
    
    public AvroMsgProducer getProducer() {
        return new KafkaProducer(this);
    }
}
