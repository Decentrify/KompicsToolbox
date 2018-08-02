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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import javax.annotation.Nullable;

/**
 * simplified
 * https://github.com/dropwizard/dropwizard/tree/master/dropwizard-client/src/main/java/io/dropwizard/client
 */
public class HttpClientConfiguration {

  private Duration connectionTimeout = Duration.ofMillis(500);

  private TlsConfiguration tlsConfiguration;

  @JsonProperty
  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  @JsonProperty("tls")
  @Nullable
  public TlsConfiguration getTlsConfiguration() {
    return tlsConfiguration;
  }

  @JsonProperty("tls")
  public void setTlsConfiguration(TlsConfiguration tlsConfiguration) {
    this.tlsConfiguration = tlsConfiguration;
  }
}
