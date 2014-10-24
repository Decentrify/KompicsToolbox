package se.sics.cm;

import se.sics.gvod.net.MsgFrameDecoder;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;

/**
 * Created by alidar on 10/23/14.
 */
public class ChunkManagerInit<T extends ComponentDefinition> extends Init<T> {

    private final ChunkManagerConfiguration config;
    private final Class<? extends MsgFrameDecoder> msgDecoderClass;

    public ChunkManagerInit(ChunkManagerConfiguration config, Class<? extends MsgFrameDecoder> msgDecoderClass) {
        super();
        this.config = config;
        this.msgDecoderClass = msgDecoderClass;
    }


    public ChunkManagerConfiguration getConfig() {
        return config;
    }

    public Class<? extends MsgFrameDecoder> getMsgDecoderClass() {
        return msgDecoderClass;
    }
}
