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
package se.sics.ktoolbox.httpsclient.builder;

import javax.net.ssl.HostnameVerifier;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import se.sics.ktoolbox.httpsclient.WebClientBuilder;

/**
 * simplified
 * https://github.com/dropwizard/dropwizard/tree/master/dropwizard-client/src/main/java/io/dropwizard/client
 */
public class HttpClientBuilder {

  private String environmentName;

  private HttpClientConfiguration configuration = new HttpClientConfiguration();

  private HostnameVerifier verifier;

  private Registry<ConnectionSocketFactory> registry;

  public HttpClientBuilder using(HttpClientConfiguration configuration) {
    this.configuration = configuration;
    return this;
  }

  public HttpClientBuilder using(HostnameVerifier verifier) {
    this.verifier = verifier;
    return this;
  }

  public HttpClientBuilder using(Registry<ConnectionSocketFactory> registry) {
    this.registry = registry;
    return this;
  }
  
  Registry<ConnectionSocketFactory> createConfiguredRegistry() {
    if (registry != null) {
      return registry;
    }

    TlsConfiguration tlsConfiguration = configuration.getTlsConfiguration();
    if (tlsConfiguration == null && verifier != null) {
      tlsConfiguration = new TlsConfiguration();
    }

    final SSLConnectionSocketFactory sslConnectionSocketFactory;
    if (tlsConfiguration == null) {
      sslConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
    } else {
      sslConnectionSocketFactory = new SimpleSSLConnectionSocketFactory(tlsConfiguration,verifier).getSocketFactory();
    }

    return RegistryBuilder.<ConnectionSocketFactory> create()
      .register("http", PlainConnectionSocketFactory.getSocketFactory())
      .register("https", sslConnectionSocketFactory)
      .build();
  }

  public CloseableHttpClient build() {
    HttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(createConfiguredRegistry());
    return createClient(org.apache.http.impl.client.HttpClientBuilder.create(), manager);
  }

  protected CloseableHttpClient createClient(
    final org.apache.http.impl.client.HttpClientBuilder builder,
    final HttpClientConnectionManager manager) {
    final Integer connectionTimeout = (int) configuration.getConnectionTimeout().toMillis();

    final RequestConfig requestConfig
      = RequestConfig.custom()
        .setConnectTimeout(connectionTimeout)
        .build();

    customizeBuilder(builder)
      .setConnectionManager(manager);

    if (verifier != null) {
      builder.setSSLHostnameVerifier(verifier);
    }

    return builder.build();
  }

  protected org.apache.http.impl.client.HttpClientBuilder customizeBuilder(
    org.apache.http.impl.client.HttpClientBuilder builder) {
    return builder;
  }
}
