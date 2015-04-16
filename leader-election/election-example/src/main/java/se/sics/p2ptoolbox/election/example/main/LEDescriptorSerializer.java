package se.sics.p2ptoolbox.election.example.main;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;

/**
 * Serializer for the application specific leader descriptor.
 *
 * Created by babbar on 2015-04-02.
 */
public class LEDescriptorSerializer implements Serializer{

    private int id;

    public LEDescriptorSerializer(int id){
        this.id = id;
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf byteBuf) {

        LeaderDescriptor leaderDescriptor = (LeaderDescriptor)o;
        byteBuf.writeBoolean(leaderDescriptor.membership);
        byteBuf.writeInt(leaderDescriptor.utility);


    }

    @Override
    public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {

        boolean membership = byteBuf.readBoolean();
        int utility = byteBuf.readInt();

        return new LeaderDescriptor(utility, membership);
    }
}
