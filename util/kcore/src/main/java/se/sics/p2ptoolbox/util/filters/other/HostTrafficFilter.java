///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.p2ptoolbox.util.filters;
//
//import org.javatuples.Pair;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.kompics.ChannelFilter;
//import se.sics.kompics.network.Address;
//import se.sics.kompics.network.Msg;
//import se.sics.ktoolbox.util.address.IntIdAddress;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class HostTrafficFilter extends ChannelFilter<Msg, Boolean> {
//
//    private final Logger LOG = LoggerFactory.getLogger("Setup");
//    private final String logPrefix = getClass().getCanonicalName();
//    private final Integer hostId;
//
//    public HostTrafficFilter(Integer hostId) {
//        super(Msg.class, true, true);
//        this.hostId = hostId;
//    }
//
//    /**
//     * @param msg
//     * @return I can only identify DecoratedAddress. If it is not a
//     * DecoratedAddress I say it doesn't match with hostId
//     */
//    @Override
//    public Boolean getValue(Msg msg) {
//        Pair<Boolean, Boolean> result = sameHost(msg);
//        return result.getValue0() && result.getValue1();
//    }
//
//    /**
//     * @param msg
//     * @return <src is same host, dst is same host>
//     */
//    public Pair<Boolean, Boolean> sameHost(Msg msg) {
//        return Pair.with(sameHost(msg.getHeader().getSource()), sameHost(msg.getHeader().getDestination()));
//    }
//
//    private boolean sameHost(Address target) {
//        if (!(target instanceof IntIdAddress)) {
//            LOG.info("{}cannot resolve filter for address type:{} - falling back to - does not match(false)",
//                    new Object[]{logPrefix, target.getClass()});
//            return false;
//        }
//        IntIdAddress src = (IntIdAddress) target;
//        return hostId.equals(src.getId());
//    }
//}
