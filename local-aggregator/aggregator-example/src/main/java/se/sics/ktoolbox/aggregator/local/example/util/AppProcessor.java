package se.sics.ktoolbox.aggregator.local.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.aggregator.global.api.PacketInfo;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfo;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;


/**
 * Processor for the information contained in the application.
 *
 * Created by babbarshaer on 2015-08-31.
 */
public class AppProcessor implements ComponentInfoProcessor {

    Logger logger   = LoggerFactory.getLogger(AppProcessor.class);

    public PacketInfo processComponentInfo(ComponentInfo componentInfo) {

        logger.debug("Going to start with the processing of the application component information.");
        if( ! (componentInfo instanceof AppComponentInfo)){

            logger.debug("The processor is not constructed for the processing of this Component Info.");
            return null;
        }

        AppComponentInfo info = (AppComponentInfo)componentInfo;
        return new IntegerPacketInfo(info.var1, info.var2);
    }
}
