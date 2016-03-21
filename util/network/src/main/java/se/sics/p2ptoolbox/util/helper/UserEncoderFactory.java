package se.sics.p2ptoolbox.util.helper;

import com.google.common.io.BaseEncoding;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.network.netty.serialization.Serializer;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

/**
 * Factory containing various helper methods for serializing
 * different objects.
 *
 * Created by babbar on 2015-07-30.
 */
public class UserEncoderFactory {

    private static Logger logger = LoggerFactory.getLogger(UserEncoderFactory.class);




    public static void writeStringLength256(ByteBuf buffer, String str) throws EncodingException {
        if(str == null) {
            writeUnsignedintAsOneByte(buffer, 0);
        } else {
            if(str.length() > 255) {
                throw new EncodingException("String length > 255 : " + str);
            }

            byte[] strBytes;
            try {
                strBytes = str.getBytes("UTF-8");
            } catch (UnsupportedEncodingException var4) {
                throw new EncodingException("Unsupported chartset when encoding string: UTF-8");
            }

            int len = strBytes.length;
            writeUnsignedintAsOneByte(buffer, len);
            buffer.writeBytes(strBytes);
        }

    }


    public static void writeUnsignedintAsOneByte(ByteBuf buffer, int value) throws EncodingException {
        if((double)value < Math.pow(2.0D, 8.0D) && value >= 0) {
            buffer.writeByte((byte)(value & 255));
        } else {
            throw new EncodingException("writeUnsignedintAsOneByte: Integer value < 0 or " + value + " is larger than 2^15");
        }
    }


    /**
     * Simple helper method to write a collection
     * to byte buffer.
     *
     * @param objCollection
     * @param serializer
     * @param buf
     */
    public static void collectionToBuff(Collection objCollection, Serializer serializer, ByteBuf buf){

        int size = objCollection.size();
        buf.writeInt(size);

        for(Object obj : objCollection){
            serializer.toBinary(obj, buf);
        }
    }


    /**
     * In cases where the object could be null, extra information
     * needs to be placed before writing the object.
     *
     * @param buf buffer
     * @param obj object
     */
    public static void checkNullAndUpdateBuff(ByteBuf buf, Object obj){

        if(obj == null){
            buf.writeBoolean(true);
        }
        else{
            buf.writeBoolean(false);
        }

    }



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
