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
package se.sics.ktoolbox.util.test;

import java.util.LinkedList;
import java.util.UUID;
import se.sics.kompics.Channel;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.ChannelFactory;
import se.sics.kompics.ChannelSelector;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.ControlPort;
import se.sics.kompics.Direct;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.MatchedHandler;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class MockComponentProxy implements ComponentProxy {

    private final LinkedList<Validator> futureValidators = new LinkedList<>();
    private final LinkedList<Validator> pastValidators = new LinkedList<>();

    public void expect(Validator validator) {
        futureValidators.add(validator);
    }

    public Validator validateNext() {
        if (pastValidators.isEmpty()) {
            return new WrongValidator("no validator");
        }
        return pastValidators.removeFirst();
    }

    public Validator validate() {
        for (Validator v : pastValidators) {
            if (!v.isValid()) {
                return v;
            }
        }
        return null;
    }

    @Override
    public <P extends PortType> void trigger(KompicsEvent e, Port<P> p) {
        if (futureValidators.isEmpty()) {
            pastValidators.add(new WrongValidator("no validator"));
        } else {
            Validator next = futureValidators.removeFirst();
            if (next instanceof EventValidator) {
                EventValidator ev = (EventValidator) next;
                ev.setFound(e);
                pastValidators.add(ev);
            } else {
                futureValidators.addFirst(next);
                pastValidators.add(new WrongValidator("expected EventValidator, found " + next.toString()));
            }
        }
    }

    @Override
    public <P extends PortType> void answer(Direct.Request event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
    }

    @Override
    public <P extends PortType> void answer(Direct.Request req, Direct.Response resp) {
        if (futureValidators.isEmpty()) {
            pastValidators.add(new WrongValidator("no validator"));
        } else {
            Validator next = futureValidators.removeFirst();
            if (next instanceof EventValidator) {
                EventValidator ev = (EventValidator) next;
                ev.setFound(resp);
                pastValidators.add(ev);
            } else {
                futureValidators.addFirst(next);
                pastValidators.add(new WrongValidator("expected EventValidator, found " + next.toString()));
            }
        }
    }

    @Override
    public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy(Component component) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelSelector<?, ?> filter) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive, ChannelSelector<?, ?> filter) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Negative<ControlPort> getControlPort() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelFactory factory) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelSelector<?, ?> selector, ChannelFactory factory) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> void disconnect(Channel<P> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) {
    }

    @Override
    public void subscribe(MatchedHandler handler, Port port) {
    }

    @Override
    public void unsubscribe(MatchedHandler handler, Port port) {
    }

    @Override
    public <E extends KompicsEvent, P extends PortType> void unsubscribe(Handler<E> handler, Port<P> port) {
    }

    @Override
    public UUID id() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <P extends PortType> Positive<P> getPositive(Class<P> portType) {
         if (futureValidators.isEmpty()) {
            pastValidators.add(new WrongValidator("no validator"));
        } else {
            Validator next = futureValidators.removeFirst();
            if (next instanceof PortValidator) {
                PortValidator pv = (PortValidator) next;
                pv.setFound(false, portType);
                pastValidators.add(pv);
            } else {
                futureValidators.addFirst(next);
                pastValidators.add(new WrongValidator("expected PortValidator, found " + next.toString()));
            }
        }

        return new MockPort();
    }

    @Override
    public <P extends PortType> Negative<P> getNegative(Class<P> portType) {
        if (futureValidators.isEmpty()) {
            pastValidators.add(new WrongValidator("no validator"));
        } else {
            Validator next = futureValidators.removeFirst();
            if (next instanceof PortValidator) {
                PortValidator pv = (PortValidator) next;
                pv.setFound(false, portType);
                pastValidators.add(pv);
            } else {
                futureValidators.addFirst(next);
                pastValidators.add(new WrongValidator("expected PortValidator, found " + next.toString()));
            }
        }

        return new MockPort();
    }

  @Override
  public <P extends PortType> Positive<P> requires(Class<P> portType) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public <P extends PortType> Negative<P> provides(Class<P> portType) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

    public static class MockPort<P extends PortType> implements Negative<P>, Positive<P> {

        @Override
        public P getPortType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void doTrigger(KompicsEvent event, int wid, ChannelCore<?> channel) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void doTrigger(KompicsEvent event, int wid, ComponentCore component) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ComponentCore getOwner() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public PortCore<P> getPair() {
            return null;
        }

        @Override
        public void setPair(PortCore<P> port) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <E extends KompicsEvent> void doSubscribe(Handler<E> handler) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void doSubscribe(MatchedHandler handler) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addChannel(ChannelCore<P> channel) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addChannel(ChannelCore<P> channel, ChannelSelector<?, ?> filter) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void removeChannel(ChannelCore<P> remotePort) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void enqueue(KompicsEvent event) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
}
