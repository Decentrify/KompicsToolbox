package se.sics.p2ptoolbox.util.helper;

/**
 * Exception occurred during the decoding
 * of serialized messaged.
 *
 * Created by babbar on 2015-07-30.
 */
public class DecodingException extends Exception{

    public DecodingException(String msg){
        super(msg);
    }
}
