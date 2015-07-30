package se.sics.p2ptoolbox.util.helper;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Factory containing various helper methods for serializing
 * different objects.
 *
 * Created by babbar on 2015-07-30.
 */
public class UserEncoderFactory {

    private static Logger logger = LoggerFactory.getLogger(UserEncoderFactory.class);




    /**
     * String encoding.
     *
     * @param buffer byte buffer
     * @param str string
     * @throws EncodingException
     */
    public static void writeStringLength65536(ByteBuf buffer, String str) throws EncodingException {

        byte[] strBytes;
        if (str == null) {
            writeUnsignedintAsTwoBytes(buffer, 0);
        } else {
            try {
                strBytes = str.getBytes(Config.STRING_CHARSET);
            } catch (UnsupportedEncodingException ex) {
                logger.warn(ex.toString());
                throw new EncodingException("Unsupported chartset when encoding string: "
                        + Config.STRING_CHARSET);
            }
            int len = strBytes.length;
            if (len > Config.DEFAULT_MTU - 42) {
                throw new EncodingException("Tried to write more bytes to "
                        + "writeString65536 than the MTU size. Attempted to write #bytes: " + len);
            }
            writeUnsignedintAsTwoBytes(buffer, len);
            buffer.writeBytes(strBytes);
        }
    }


    /**
     * Encoding an int as two bytes.
     *
     * @param buffer buffer
     * @param value value
     * @throws EncodingException
     */
    public static void writeUnsignedintAsTwoBytes(ByteBuf buffer, int value) throws EncodingException {
        byte[] result = new byte[2];
        if ((value >= Math.pow(2, 16)) || (value < 0)) {
            throw new EncodingException("writeUnsignedintAsTwoBytes: + Integer value < 0 or " + value + " is larger than 2^31");
        }
        result[0] = (byte) ((value >>> 8) & 0xFF);
        result[1] = (byte) (value & 0xFF);
        buffer.writeBytes(result);
    }



}
