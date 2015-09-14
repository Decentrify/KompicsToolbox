package se.sics.ktoolbox.aggregator.local.api;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Timeout indication the aggregation is over
 * and the data needs to be sent for filtering.
 *
 * Created by babbar on 2015-08-31.
 */
public class AggregationTimeout extends Timeout{

    public AggregationTimeout(SchedulePeriodicTimeout request) {
        super(request);
    }
}
