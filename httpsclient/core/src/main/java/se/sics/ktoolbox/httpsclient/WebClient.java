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
package se.sics.ktoolbox.httpsclient;

import java.io.Closeable;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Future;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WebClient implements Closeable {

  public final Client client;
  private Class respContentClass;
  private String target;
  private String path;
  private Entity payload;
  private String mediaType = MediaType.APPLICATION_JSON;

  private WebClient(Client client) {
    this.client = client;
    this.payload = Entity.entity("", mediaType);
  }

  public WebClient setTarget(String target) {
    this.target = target;
    return this;
  }

  public WebClient setPath(String path) {
    this.path = path;
    return this;
  }

  public WebClient setMediaType(String mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public WebClient setPayload(Object object) {
    this.payload = Entity.entity(object, mediaType);
    return this;
  }

  @Override
  public void close() {
    if (client != null) {
      client.close();
    }
  }

  public WebResponse doGet() {
    performSanityCheck();
    Response response = client.target(target).path(path).request(mediaType).get();
    return new WebResponse(response);
  }
  
  public AsyncWebResponse doAsyncGet() {
    performSanityCheck();
    Future<Response> response = client.target(target).path(path).request(mediaType).async().get();
    return new AsyncWebResponse(response);
  }

  public WebResponse doPost() {
    performSanityCheck();
    Response response = client.target(target).path(path).request(mediaType).post(payload);
    return new WebResponse(response);
  }
  
  public AsyncWebResponse doAsyncPost() {
    performSanityCheck();
    Future<Response> response = client.target(target).path(path).request(mediaType).async().post(payload);
    return new AsyncWebResponse(response);
  }

  public WebResponse doPut() {
    performSanityCheck();
    Response response = client.target(target).path(path).request(mediaType).put(payload);
    return new WebResponse(response);
  }

  public AsyncWebResponse doAsyncPut() {
    performSanityCheck();
    Future<Response> response = client.target(target).path(path).request(mediaType).async().put(payload);
    return new AsyncWebResponse(response);
  }
  
  public WebResponse doDelete() {
    performSanityCheck();
    Response response = client.target(target).path(path).request(mediaType).delete();
    return new WebResponse(response);
  }
  
  public AsyncWebResponse doAsyncDelete() {
    performSanityCheck();
    Future<Response> response = client.target(target).path(path).request(mediaType).async().delete();
    return new AsyncWebResponse(response);
  }

  private void performSanityCheck() {
    if (client == null) {
      throw new IllegalStateException("Client not created.");
    }

    if (target == null || target.isEmpty()) {
      throw new IllegalStateException("Target not set.");
    }

    if (path == null || path.isEmpty()) {
      throw new IllegalStateException("Path not set.");
    }
  }

  public String getFullPath() {
    return this.target + "/" + this.path;
  }

  public static WebClient httpsInstance() {
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts(), new java.security.SecureRandom());
      Client client = ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier(acceptAnyHost()).build();
      return new WebClient(client);
    } catch (NoSuchAlgorithmException | KeyManagementException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static <T> WebClient httpInstance() {
    Client client = ClientBuilder.newClient();
    return new WebClient(client);
  }

  private static TrustManager[] trustAllCerts() {
    return new TrustManager[]{
      new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
    };
  }

  private static HostnameVerifier acceptAnyHost() {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String string, SSLSession ssls) {
        return true;
      }
    };
  }
}
