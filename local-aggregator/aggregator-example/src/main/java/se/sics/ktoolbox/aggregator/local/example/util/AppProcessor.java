package se.sics.ktoolbox.aggregator.local.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;


/**
 * Processor for the information contained in the application.
 *
 * Created by babbarshaer on 2015-08-31.
 */
public class AppProcessor implements ComponentInfoProcessor<AppComponentInfo, IntegerPacketInfo> {

    Logger logger   = LoggerFactory.getLogger(AppProcessor.class);

    @Override
    public IntegerPacketInfo processComponentInfo(AppComponentInfo componentInfo) {

        logger.debug("Processing the component information.");
        return new IntegerPacketInfo(componentInfo.var1, componentInfo.var2);
    }
}
