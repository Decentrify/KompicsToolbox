/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.videostream.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.videostream.VideoStreamManager;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RangeCapableMp4Handler extends BaseHandler {

    private static final int RANGE_CODE = 206;

    private static final Logger log = LoggerFactory.getLogger(RangeCapableMp4Handler.class);

    private final VideoStreamManager playMngr;

    public RangeCapableMp4Handler(VideoStreamManager playMngr) {
        this.playMngr = playMngr;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.debug("method:{} request headers:{}",
                new Object[]{exchange.getRequestMethod(), exchange.getRequestHeaders().entrySet()});
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            long contentLength = playMngr.getLength();
            Pair<Long, Long> range = extractRange(exchange.getRequestHeaders().get("Range"));
            if (range == null) {
                log.error("could not parse range");
                System.exit(1);
                return;
            }
            log.info("method:{} from:{} for range[{},{}]", 
                    new Object[]{exchange.getRequestMethod(), exchange.getRequestHeaders().get("referer"), range.getValue0(), range.getValue1()});
            byte[] content;
            if (range.getValue1() == null) {
                content = playMngr.getContent(range.getValue0());
            } else {
                content = playMngr.getContent(range.getValue0(), range.getValue1());
            }
            
            long rangeLength = content.length;
            long rangeEnd = range.getValue0() + (rangeLength == 0 ? 0 : rangeLength - 1);
            log.info("sending range[{},{}] range length:{}", new Object[]{range.getValue0(), rangeEnd, content.length});
            setRangeHeaders(exchange.getResponseHeaders(), contentLength, range.getValue0(), rangeEnd, rangeLength);
            exchange.sendResponseHeaders(RANGE_CODE, rangeLength);
            exchange.getResponseBody().write(content);
            exchange.getResponseBody().flush();
            exchange.getResponseBody().close();
            log.debug("method:{} response code:{} response headers:{}",
                    new Object[]{exchange.getRequestMethod(), exchange.getResponseCode(), exchange.getResponseHeaders().entrySet()});
        }
    }

    private void setRangeHeaders(Headers responseHeaders, long contentLength, long rangeStart, long rangeEnd, long rangeLength) {
        responseHeaders.set("Content-Type", "video/mp4");
        responseHeaders.set("Content-Length", "" + rangeLength);
        responseHeaders.set("Accept-Ranges", "bytes");
        responseHeaders.set("Content-Range", "bytes " + rangeStart + "-" + (rangeEnd) + "/" + contentLength);
        DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        Date date = new Date();
        String dateStr = dateFormat.format(date);
        responseHeaders.set("Date", dateStr);
//        responseHeaders.set("X-Mod-H264-Streaming", "version=2.2.7");

        //check these later
//        responseHeaders.set("Server", "lighttpd/1.4.26"); // Gvod 1.0
        responseHeaders.set("Connection", "Keep-Alive");
        responseHeaders.set("Keep-Alive", "timeout=600, max=99");
    }

    private Pair<Long, Long> extractRange(List<String> sList) {
        if (sList.isEmpty()) {
            return null;
        }
        String sRange = sList.get(0);
        if (!sRange.startsWith("bytes=")) {
            return null;
        }
        sRange = sRange.substring("bytes=".length());
        if (!sRange.contains("-")) {
            return null;
        }
        String start = sRange.substring(0, sRange.indexOf("-"));
        String end = sRange.substring(sRange.indexOf("-") + 1);

        try {
            Long rangeStart = Long.parseLong(start);
            if (end.equals("")) {
                return Pair.with(rangeStart, null);
            }
            Long rangeEnd = Long.parseLong(end);
            return Pair.with(rangeStart, rangeEnd);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
