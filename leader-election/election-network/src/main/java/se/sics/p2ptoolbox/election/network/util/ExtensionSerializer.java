package se.sics.p2ptoolbox.election.network.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;

/**
 * Serializer for the extension request sent during the leader election
 * protocol.
 *
 * Created by babbar on 2015-04-15.
 */
public class ExtensionSerializer implements Serializer{

    private int id;

    public ExtensionSerializer(int id){
        this.id = id;
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf byteBuf) {

    }

    @Override
    public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
        return null;
    }
}
