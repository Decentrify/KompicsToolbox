package se.sics.ktoolbox.aggregator.server.core.util;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Timeout indicating the global aggregator to stop aggregating
 * and start processing the information.
 *
 * Created by babbarshaer on 2015-09-01.
 */
public class AggregationTimeout extends Timeout{

    public AggregationTimeout(SchedulePeriodicTimeout request) {
        super(request);
    }

}
