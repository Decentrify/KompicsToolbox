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
package se.sics.ktoolbox.webclient.hops;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import se.sics.ktoolbox.webclient.WebClient;
import se.sics.ktoolbox.webclient.WebResponse;
import se.sics.ktoolbox.webclient.hops.dto.DelaReportDTO;
import se.sics.ktoolbox.webclient.hops.dto.ReportDTO;
import se.sics.ktoolbox.webclient.hops.dto.SearchServiceDTO;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HopssiteClient {

  public static void main(String[] args) {
    //search(args[0]);
    WebClient.setBuilder(new WebClient.BasicBuilder());
    search("test");
  }
  
  private static void search(String term) {
    SearchServiceDTO.Params searchParam = new SearchServiceDTO.Params(term);
    WebResponse resp = WebClient.httpsInstance()
      .setTarget(Hopssite.Target.bbc5_test())
      .setPath(Hopssite.Path.search())
      .setPayload(searchParam, MediaType.APPLICATION_JSON_TYPE)
      .doPost();
    SearchServiceDTO.SearchResult result = resp.readContent(SearchServiceDTO.SearchResult.class);
    System.out.println("nr hits:" + result.getNrHits());
  }
  
  private static void reportData() {
    ReportDTO report = new ReportDTO();
    report.addValue("val1");
    report.addValue("val2");
    DelaReportDTO delaReport = new DelaReportDTO("1", "2", "3", report.toString());
    WebResponse resp = WebClient.httpsInstance()
      .setTarget(Hopssite.Target.bbc5_test())
      .setPath(Hopssite.Path.reportDataValues())
      .setPayload(delaReport, MediaType.APPLICATION_JSON_TYPE)
      .doPost();
    String result = resp.readContent(String.class);
    System.out.println("result:" + result);
  }
  
  private static void reportDownload() {
    ReportDTO report = new ReportDTO();
    report.addValue("val1");
    report.addValue("val2");
    DelaReportDTO delaReport = new DelaReportDTO("1", "2", "3", report.toString());
    WebResponse resp = WebClient.httpsInstance()
      .setTarget(Hopssite.Target.bbc5_test())
      .setPath(Hopssite.Path.reportDownloadValues())
      .setPayload(delaReport, MediaType.APPLICATION_JSON_TYPE)
      .doPost();
    String result = resp.readContent(String.class);
    System.out.println("result:" + result);
  }
}
