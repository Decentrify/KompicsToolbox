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

import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Future;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.glassfish.jersey.apache.connector.LocalizationMessages;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.message.internal.Statuses;

public class SimpleApacheConnector implements Connector {


    private final CloseableHttpClient client;
    private final RequestConfig defaultRequestConfig;
    private final boolean chunkedEncodingEnabled;

    public SimpleApacheConnector(CloseableHttpClient client) {
        this.client = client;
        this.defaultRequestConfig = null;
        this.chunkedEncodingEnabled = true;
    }

    @Override
    public ClientResponse apply(ClientRequest jerseyRequest) {
        try {
            final HttpUriRequest apacheRequest = buildApacheRequest(jerseyRequest);
            final CloseableHttpResponse apacheResponse = client.execute(apacheRequest);

            final StatusLine statusLine = apacheResponse.getStatusLine();
            final String reasonPhrase = Strings.nullToEmpty(statusLine.getReasonPhrase());
            final Response.StatusType status = Statuses.from(statusLine.getStatusCode(), reasonPhrase);

            final ClientResponse jerseyResponse = new ClientResponse(status, jerseyRequest);
            for (Header header : apacheResponse.getAllHeaders()) {
                jerseyResponse.getHeaders().computeIfAbsent(header.getName(), k -> new ArrayList<>())
                    .add(header.getValue());
            }

            final HttpEntity httpEntity = apacheResponse.getEntity();
            jerseyResponse.setEntityStream(httpEntity != null ? httpEntity.getContent() :
                    new ByteArrayInputStream(new byte[0]));

            return jerseyResponse;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private HttpUriRequest buildApacheRequest(ClientRequest jerseyRequest) {
        final RequestBuilder builder = RequestBuilder
                .create(jerseyRequest.getMethod())
                .setUri(jerseyRequest.getUri())
                .setEntity(getHttpEntity(jerseyRequest));
        for (String headerName : jerseyRequest.getHeaders().keySet()) {
            builder.addHeader(headerName, jerseyRequest.getHeaderString(headerName));
        }

        final Optional<RequestConfig> requestConfig = addJerseyRequestConfig(jerseyRequest);
        requestConfig.ifPresent(builder::setConfig);

        return builder.build();
    }

    private Optional<RequestConfig> addJerseyRequestConfig(ClientRequest clientRequest) {
        final Integer timeout = clientRequest.resolveProperty(ClientProperties.READ_TIMEOUT, Integer.class);
        final Integer connectTimeout = clientRequest.resolveProperty(ClientProperties.CONNECT_TIMEOUT, Integer.class);
        final Boolean followRedirects = clientRequest.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, Boolean.class);

        if (timeout != null || connectTimeout != null || followRedirects != null) {
            final RequestConfig.Builder requestConfig = RequestConfig.copy(defaultRequestConfig);

            if (timeout != null) {
                requestConfig.setSocketTimeout(timeout);
            }

            if (connectTimeout != null) {
                requestConfig.setConnectTimeout(connectTimeout);
            }

            if (followRedirects != null) {
                requestConfig.setRedirectsEnabled(followRedirects);
            }

            return Optional.of(requestConfig.build());
        }

        return Optional.empty();
    }

    protected HttpEntity getHttpEntity(ClientRequest jerseyRequest) {
        if (jerseyRequest.getEntity() == null) {
            return null;
        }

        return chunkedEncodingEnabled ? new JerseyRequestHttpEntity(jerseyRequest) :
                new BufferedJerseyRequestHttpEntity(jerseyRequest);
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "Apache-HttpClient/";
    }

    @Override
    public void close() {
        // Should not close the client here, because it's managed by the Dropwizard environment
    }

    private static class JerseyRequestHttpEntity extends AbstractHttpEntity {

        private ClientRequest clientRequest;

        private JerseyRequestHttpEntity(ClientRequest clientRequest) {
            this.clientRequest = clientRequest;
            setChunked(true);
        }

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public InputStream getContent() throws IOException {
            // Shouldn't be called
            throw new UnsupportedOperationException("Reading from the entity is not supported");
        }

        @Override
        public void writeTo(final OutputStream outputStream) throws IOException {
            clientRequest.setStreamProvider(contentLength -> outputStream);
            clientRequest.writeEntity();
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

    }

    private static class BufferedJerseyRequestHttpEntity extends AbstractHttpEntity {

        private static final int BUFFER_INITIAL_SIZE = 512;
        private byte[] buffer;

        private BufferedJerseyRequestHttpEntity(ClientRequest clientRequest) {
            final ByteArrayOutputStream stream = new ByteArrayOutputStream(BUFFER_INITIAL_SIZE);
            clientRequest.setStreamProvider(contentLength -> stream);
            try {
                clientRequest.writeEntity();
            } catch (IOException e) {
                throw new ProcessingException(LocalizationMessages.ERROR_BUFFERING_ENTITY(), e);
            }
            buffer = stream.toByteArray();
            setChunked(false);
        }

        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public long getContentLength() {
            return buffer.length;
        }

        @Override
        public InputStream getContent() throws IOException {
            // Shouldn't be called
            throw new UnsupportedOperationException("Reading from the entity is not supported");
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            outstream.write(buffer);
            outstream.flush();
        }

        @Override
        public boolean isStreaming() {
            return false;
        }
    }
}

