package se.sics.p2ptoolbox.election.network.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;

/**
 * Main Wrapper class for the serializer of the objects passed during promise phase
 * of the leader election.
 *
 * Created by babbar on 2015-04-15.
 */
public class PromiseSerializer {


    public static class Request implements Serializer {

        private int id;

        public Request(int id){
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


    public static class Response implements Serializer{


        private int id;

        public Response(int id){
            this.id = id;
        }

        @Override
        public int identifier() {
            return 0;
        }

        @Override
        public void toBinary(Object o, ByteBuf byteBuf) {

        }

        @Override
        public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {
            return null;
        }
    }


}
