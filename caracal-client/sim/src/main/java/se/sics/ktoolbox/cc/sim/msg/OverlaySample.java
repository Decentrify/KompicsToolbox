package se.sics.ktoolbox.cc.sim.msg;

import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Set;

/**
 * Object containing the information about the overlay
 * for which the information needs to be fetched.
 *
 * Created by babbar on 2015-08-15.
 */
public class OverlaySample {

    public static class Request {

        public final byte[] overlayIdentifier;

        public Request(byte[] overlayIdentifier){
            this.overlayIdentifier = overlayIdentifier;
        }
    }

    public static class Response {

        public final byte[] overlayIdentifier;
        public final Set<DecoratedAddress> neighbors;

        public Response(byte[] identifier, Set<DecoratedAddress> neighbors){

            this.overlayIdentifier = identifier;
            this.neighbors = neighbors;
        }
    }

}
