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
package se.sics.ktoolbox.webclient.builder;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

/**
 * simplified
 * https://github.com/dropwizard/dropwizard/tree/master/dropwizard-client/src/main/java/io/dropwizard/client
 */
public class SimpleSSLConnectionSocketFactory {

  private final TlsConfiguration configuration;

  private final HostnameVerifier verifier;

  public SimpleSSLConnectionSocketFactory(TlsConfiguration configuration, @Nullable HostnameVerifier verifier) {
    this.configuration = configuration;
    this.verifier = verifier;
  }

  public SSLConnectionSocketFactory getSocketFactory() throws SSLInitializationException {
    return new SSLConnectionSocketFactory(buildSslContext(), getSupportedProtocols(), getSupportedCiphers(),
      chooseHostnameVerifier());
  }

  @Nullable
  private String[] getSupportedCiphers() {
    return null;
  }

  @Nullable
  private String[] getSupportedProtocols() {
    return null;
  }

  private HostnameVerifier chooseHostnameVerifier() {
    if (configuration.isVerifyHostname()) {
      return verifier != null ? verifier : SSLConnectionSocketFactory.getDefaultHostnameVerifier();
    } else {
      return new NoopHostnameVerifier();
    }
  }

  private SSLContext buildSslContext() throws SSLInitializationException {
    final SSLContext sslContext;
    try {
      final SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
      loadTrustMaterial(sslContextBuilder);
      sslContext = sslContextBuilder.build();
    } catch (Exception e) {
      throw new SSLInitializationException(e.getMessage(), e);
    }
    return sslContext;
  }

  private void loadTrustMaterial(SSLContextBuilder sslContextBuilder) throws Exception {
    TrustStrategy trustStrategy = null;
    if (configuration.isTrustSelfSignedCertificates()) {
      trustStrategy = new TrustSelfSignedStrategy();
    }
    sslContextBuilder.loadTrustMaterial(trustStrategy);
  }
}
