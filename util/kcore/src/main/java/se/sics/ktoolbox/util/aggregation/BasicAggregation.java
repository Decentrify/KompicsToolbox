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
package se.sics.ktoolbox.util.aggregation;

import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.update.view.ViewUpdate;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicAggregation {
    public static void registerPorts() {
        //timer
        AggregationRegistry.registerPositive(Timeout.class, Timer.class);
        AggregationRegistry.registerNegative(ScheduleTimeout.class, Timer.class);
        AggregationRegistry.registerNegative(CancelTimeout.class, Timer.class);
        AggregationRegistry.registerNegative(SchedulePeriodicTimeout.class, Timer.class);
        AggregationRegistry.registerNegative(CancelPeriodicTimeout.class, Timer.class);
        //network
        AggregationRegistry.registerNegative(Msg.class, Network.class);
        AggregationRegistry.registerPositive(Msg.class, Network.class);
        AggregationRegistry.registerNegative(MessageNotify.Req.class, Network.class); //request = negative
        AggregationRegistry.registerPositive(MessageNotify.Resp.class, Network.class);
        //address update
        AggregationRegistry.registerNegative(AddressUpdate.Request.class, AddressUpdatePort.class);
        AggregationRegistry.registerPositive(AddressUpdate.Indication.class, AddressUpdatePort.class);
        //view update
        AggregationRegistry.registerNegative(ViewUpdate.Request.class, ViewUpdatePort.class);
        AggregationRegistry.registerPositive(ViewUpdate.Indication.class, ViewUpdatePort.class);
    }
}
