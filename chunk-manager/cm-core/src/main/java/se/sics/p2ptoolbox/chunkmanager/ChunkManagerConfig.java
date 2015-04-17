package se.sics.p2ptoolbox.chunkmanager;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alidar on 10/23/14.
 */
public class ChunkManagerConfig {

    private static final Logger log = LoggerFactory.getLogger(ChunkManagerComp.class);

    public final long cleanupTimeout;
    public final int datagramUsableSize;

    public ChunkManagerConfig(long cleanupTimeout, int datagramUsableSize) {
        this.cleanupTimeout = cleanupTimeout;
        this.datagramUsableSize = datagramUsableSize;
    }

    public ChunkManagerConfig(Config config) {
        try {
            cleanupTimeout = config.getLong("chunk-manager.cleanupTimeout");
            datagramUsableSize = config.getInt("chunk-manager.datagramUsableSize");
            log.info("config - cleanupTimeout:{}", cleanupTimeout);
        } catch (ConfigException.Missing ex) {
            log.error("missing configuration:{}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
