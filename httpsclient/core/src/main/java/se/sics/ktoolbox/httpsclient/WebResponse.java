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

import com.google.gson.Gson;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

  public boolean statusOk() {
    Response.Status.Family status = response.getStatusInfo().getFamily();
    return status == Response.Status.Family.INFORMATIONAL || status == Response.Status.Family.SUCCESSFUL;
  }

  private void checkMediaType() {
    if (!response.getMediaType().getSubtype().equals(MediaType.APPLICATION_JSON_TYPE.getSubtype())) {
      throw new IllegalStateException("expected json type, found:" + response.getMediaType().getSubtype());
    }
  }
}
