package se.sics.cm.timeout;

import se.sics.cm.model.ChunkContainer;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;

/**
 * Created by alidar on 10/23/14.
 */
public class ChunkedMessageReceiveTimeout extends Timeout {

    ChunkContainer chunkContainer;

    public ChunkedMessageReceiveTimeout(SchedulePeriodicTimeout request, ChunkContainer chunkContainer) {
        super(request);
        this.chunkContainer = chunkContainer;
    }

    public ChunkedMessageReceiveTimeout(ScheduleTimeout request, ChunkContainer chunkContainer) {
        super(request);
        this.chunkContainer = chunkContainer;
    }

}
