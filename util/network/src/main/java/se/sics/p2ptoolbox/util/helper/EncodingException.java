package se.sics.p2ptoolbox.util.helper;

/**
 * Exception occurred during serialization of the
 * message.
 *
 * Created by babbar on 2015-07-30.
 */
public class EncodingException extends Exception{

    public EncodingException(String msg){
        super(msg);
    }
}
