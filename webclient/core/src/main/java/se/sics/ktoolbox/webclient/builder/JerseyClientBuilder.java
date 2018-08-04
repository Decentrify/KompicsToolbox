/*
 * Copyright (C) 2016 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2016 Royal Institute of Technology (KTH)
 *
 * Dozy is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.webclient.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import javax.annotation.Nullable;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.net.ssl.HostnameVerifier;
import org.apache.http.config.Registry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import se.sics.ktoolbox.webclient.WebClient;
import se.sics.ktoolbox.webclient.WebClientBuilder;

/**
 * simplified
 * https://github.com/dropwizard/dropwizard/tree/master/dropwizard-client/src/main/java/io/dropwizard/client
 */
public class JerseyClientBuilder implements WebClientBuilder {

  private final List<Object> singletons = new ArrayList<>();
  private final List<Class<?>> providers = new ArrayList<>();
  private final Map<String, Object> properties = new LinkedHashMap<>();
  private JerseyClientConfiguration configuration = new JerseyClientConfiguration();

  private HttpClientBuilder apacheHttpClientBuilder;

  @Nullable
  private ObjectMapper objectMapper;

  public JerseyClientBuilder() {
    this.apacheHttpClientBuilder = new HttpClientBuilder();
  }

  public void setApacheHttpClientBuilder(HttpClientBuilder apacheHttpClientBuilder) {
    this.apacheHttpClientBuilder = apacheHttpClientBuilder;
  }

  public JerseyClientBuilder withProvider(Object provider) {
    singletons.add(requireNonNull(provider));
    return this;
  }

  public JerseyClientBuilder withProvider(Class<?> klass) {
    providers.add(requireNonNull(klass));
    return this;
  }

  public JerseyClientBuilder withProperty(String propertyName, Object propertyValue) {
    properties.put(propertyName, propertyValue);
    return this;
  }

  public JerseyClientBuilder using(JerseyClientConfiguration configuration) {
    this.configuration = configuration;
    apacheHttpClientBuilder.using(configuration);
    return this;
  }

  public JerseyClientBuilder using(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    return this;
  }

  public JerseyClientBuilder using(HostnameVerifier verifier) {
    apacheHttpClientBuilder.using(verifier);
    return this;
  }

  public JerseyClientBuilder using(Registry<ConnectionSocketFactory> registry) {
    apacheHttpClientBuilder.using(registry);
    return this;
  }

  public Client build() {
    if (objectMapper == null) {
      throw new IllegalStateException("Must have an object mapper");
    }

    return build(objectMapper);
  }

  private Client build(ObjectMapper objectMapper) {
    final Client client = ClientBuilder.newClient(buildConfig(objectMapper));
//    client.register(new JerseyIgnoreRequestUserAgentHeaderFilter());
    return client;
  }

  private Configuration buildConfig(final ObjectMapper objectMapper) {
    final ClientConfig config = new ClientConfig();

    for (Object singleton : this.singletons) {
      config.register(singleton);
    }

    for (Class<?> provider : this.providers) {
      config.register(provider);
    }

    config.register(new JacksonFeature(objectMapper));

    for (Map.Entry<String, Object> property : this.properties.entrySet()) {
      config.property(property.getKey(), property.getValue());
    }

    ConnectorProvider connectorProvider = new ApacheConnectorProvider();
    config.connectorProvider(connectorProvider);

    return config;
  }

  @Override
  public WebClient httpsInstance() {
    return new WebClient(build());
  }

  @Override
  public WebClient httpInstance() {
    return new WebClient(build());
  }
}
