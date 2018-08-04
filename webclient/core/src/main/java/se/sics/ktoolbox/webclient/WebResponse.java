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
package se.sics.ktoolbox.webclient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import se.sics.ktoolbox.webclient.WebClient.ContentException;
import se.sics.ktoolbox.util.trysf.Try;
import se.sics.ktoolbox.util.trysf.TryHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WebResponse {

  public final Response response;

  public WebResponse(Response response) {
    this.response = response;
  }

  public <C extends Object> C readContent(Class<C> contentType) {
    Response.Status.Family status = response.getStatusInfo().getFamily();
    try {
      if (statusOk()) {
        checkMediaType();
        String stringContent = response.readEntity(String.class);
        Gson gson = new Gson();
        C content = gson.fromJson(stringContent, contentType);
        return content;
      } else {
        throw new IllegalStateException(status.toString());
      }
    } catch (ProcessingException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  public <C extends Object> C readErrorDetails(Class<C> contentType) {
    try {
      checkMediaType();
      String stringContent = response.readEntity(String.class);
      Gson gson = new Gson();
      C content = gson.fromJson(stringContent, contentType);
      return content;
    } catch (ProcessingException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  public boolean statusOk() {
    Response.Status.Family status = response.getStatusInfo().getFamily();
    return status == Response.Status.Family.INFORMATIONAL || status == Response.Status.Family.SUCCESSFUL;
  }

  private void checkMediaType() {
    if (!response.getMediaType().getSubtype().equals(MediaType.APPLICATION_JSON_TYPE.getSubtype())) {
      throw new IllegalStateException("expected json type, found:" + response.getMediaType().getSubtype());
    }
  }

  // TRY
  public static <C> BiFunction<WebResponse, Throwable, Try<C>>
    readContent(Class<C> contentType, Function<String, Throwable> serverExceptionMapper) {
    return TryHelper.tryFSucc1((WebResponse wrep) -> {
      Response response = wrep.response;
      if (!response.getMediaType().getSubtype().equals(MediaType.APPLICATION_JSON_TYPE.getSubtype())) {
        return new Try.Failure(new ContentException("expected json type, found:" + response.getMediaType().getSubtype()));
      }
      Response.Status.Family status = response.getStatusInfo().getFamily();
      try {
        if (!(status == Response.Status.Family.INFORMATIONAL || status == Response.Status.Family.SUCCESSFUL)) {
          String stringServerEx = response.readEntity(String.class);
          Throwable serverEx = serverExceptionMapper.apply(stringServerEx);
          return new Try.Failure(serverEx);
        } else {
          String stringContent = response.readEntity(String.class);
          C content = new Gson().fromJson(stringContent, contentType);
          return new Try.Success(content);
        }
      } catch (ProcessingException | JsonSyntaxException ex) {
        return new Try.Failure(new ContentException(ex));
      }
    }
    );
  }
}
