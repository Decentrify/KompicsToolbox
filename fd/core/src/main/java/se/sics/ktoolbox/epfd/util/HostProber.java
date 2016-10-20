/* 
 * This file is part of the CaracalDB distributed storage system.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.epfd.util;

import java.util.HashMap;
import java.util.UUID;
import se.sics.ktoolbox.epfd.EPFDService;
import se.sics.ktoolbox.epfd.event.EPFDFollow;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Lars Kroll <lkroll@sics.se>
 */
public class HostProber {

    private final EPFDService epfd;
    private final KAddress probedHost;
    private final HostResponseTime times;
    private final HashMap<Identifier, EPFDFollow> requests = new HashMap<>();

    private UUID nextPingTid;
    private UUID pongTid;
    private boolean suspected;

    public HostProber(EPFDService epfd, KAddress probedHost, long minRto) {
        this.epfd = epfd;
        this.probedHost = probedHost;
        times = new HostResponseTime(minRto);
        suspected = false;
    }
    
    public int followers() {
        return requests.size();
    }

    public void start() {
        nextPingTid = epfd.nextPing(suspected, probedHost);
    }

    public void ping() {
        nextPingTid = epfd.nextPing(suspected, probedHost);
        pongTid = epfd.ping(System.nanoTime(), probedHost, times.getRTO());
    }

    public void pong(Identifier id, long ts) {
        if(!pongTid.equals(id)) {
            return; //late
        }
        long RTT = System.nanoTime() - ts;
        times.updateRTO(RTT);

        if (suspected == true) {
            suspected = false;
            reviseSuspicion();
        }
    }

    public void pongTimeout() {
        if (suspected == false) {
            suspected = true;
            times.timedOut();
            suspect();
        }
    }

    private void suspect() {
        for (EPFDFollow req : requests.values()) {
            epfd.answerRequest(req, req.suspect());
        }
    }

    private void reviseSuspicion() {
        for (EPFDFollow req : requests.values()) {
            epfd.answerRequest(req, req.restore());
        }
    }

    public void stop() {
        epfd.stop(nextPingTid, pongTid);
    }

    public void addRequest(EPFDFollow request) {
        requests.put(request.id, request);
    }

    public boolean removeRequest(Identifier requestId) {
        requests.remove(requestId);
        return requests.isEmpty();
    }

    public boolean hasRequest(Identifier requestId) {
        return requests.containsKey(requestId);
    }
}
