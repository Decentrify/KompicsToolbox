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
package se.sics.ktoolbox.nutil.fsm.genericsetup;

import java.util.List;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class GenericSetup {

  private static final Logger LOG = LoggerFactory.getLogger(GenericSetup.class);

  public static void portsAndHandledEvents(ComponentProxy proxy,
    List<Pair<Class, List<Pair<OnEventAction, Class>>>> positivePorts,
    List<Pair<Class, List<Pair<OnEventAction, Class>>>> negativePorts,
    List<Pair<OnMsgAction, Class>> positiveNetworkMsgs,
    List<Pair<OnMsgAction, Class>> negativeNetworkMsgs) {

    for (Pair<Class, List<Pair<OnEventAction, Class>>> e : positivePorts) {
      LOG.info("positive port:{}", e.getValue0());
      Positive port = proxy.requires(e.getValue0());
      setupPort(proxy, port, e.getValue1());
    }
    if (!positiveNetworkMsgs.isEmpty()) {
      Positive port = proxy.requires(Network.class);
      setupNetworkPort(proxy, port, positiveNetworkMsgs);
    }
    for (Pair<Class, List<Pair<OnEventAction, Class>>> e : negativePorts) {
      LOG.info("negative port:{}", e.getValue0());
      Negative port = proxy.provides(e.getValue0());
      setupPort(proxy, port, e.getValue1());
    }
    if (!negativeNetworkMsgs.isEmpty()) {
      Negative port = proxy.provides(Network.class);
      setupNetworkPort(proxy, port, negativeNetworkMsgs);
    }
  }

  public static void handledEvents(ComponentProxy proxy,
    List<Pair<Class, List<Pair<OnEventAction, Class>>>> positivePorts,
    List<Pair<Class, List<Pair<OnEventAction, Class>>>> negativePorts,
    List<Pair<OnMsgAction, Class>> positiveNetworkMsgs,
    List<Pair<OnMsgAction, Class>> negativeNetworkMsgs) {

    for (Pair<Class, List<Pair<OnEventAction, Class>>> e : positivePorts) {
      LOG.info("positive port:{}", e.getValue0());
      Positive port = proxy.getNegative(e.getValue0()).getPair();
      setupPort(proxy, port, e.getValue1());
    }
    if (!positiveNetworkMsgs.isEmpty()) {
      Positive port = proxy.getNegative(Network.class).getPair();
      setupNetworkPort(proxy, port, positiveNetworkMsgs);
    }
    for (Pair<Class, List<Pair<OnEventAction, Class>>> e : negativePorts) {
      LOG.info("negative port:{}", e.getValue0());
      Negative port = proxy.getPositive(e.getValue0()).getPair();
      setupPort(proxy, port, e.getValue1());
    }
    if (!negativeNetworkMsgs.isEmpty()) {
      Negative port = proxy.getPositive(Network.class).getPair();
      setupNetworkPort(proxy, port, negativeNetworkMsgs);
    }
  }

  private static void setupPort(ComponentProxy proxy, Port port, List<Pair<OnEventAction, Class>> handledEvents) {
    for (final Pair<OnEventAction, Class> e : handledEvents) {
      Handler handler = new Handler(e.getValue1()) {
        @Override
        public void handle(KompicsEvent event) {
          e.getValue0().handle(event);
        }
      };
      proxy.subscribe(handler, port);
    }
  }

  private static void setupNetworkPort(ComponentProxy proxy, Port port, List<Pair<OnMsgAction, Class>> handledMsgs) {
    for (final Pair<OnMsgAction, Class> e : handledMsgs) {
      ClassMatchedHandler handler
        = new ClassMatchedHandler<Object, KContentMsg<KAddress, KHeader<KAddress>, Object>>() {

          @Override
          public void handle(Object content, KContentMsg<KAddress, KHeader<KAddress>, Object> container) {
            e.getValue0().handle(content, container);
          }
        };
      proxy.subscribe(handler, port);
    }
  }
}
