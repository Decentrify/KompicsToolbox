package se.sics.p2ptoolbox.election.network.util;

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.p2ptoolbox.util.helper.EncodingException;
import se.sics.p2ptoolbox.util.helper.UserDecoderFactory;
import se.sics.p2ptoolbox.util.helper.UserEncoderFactory;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;


/**
 * Public Key Serializer.
 *
 * Created by babbar on 2015-04-02.
 */
public class PublicKeySerializer implements Serializer {

    private int id;

    public PublicKeySerializer(int id){
        this.id = id;
    }


    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf byteBuf) {

        PublicKey publicKey = (PublicKey)o;

        try{
            if(publicKey == null){
                UserEncoderFactory.writeStringLength65536(byteBuf, "");
            }
            else{
                UserEncoderFactory.writeStringLength65536(byteBuf,  BaseEncoding.base64().encode(publicKey.getEncoded()));
            }
        }
        catch (EncodingException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Object fromBinary(ByteBuf byteBuf, Optional<Object> optional) {


        try {
            String stringKey = UserDecoderFactory.readStringLength65536(byteBuf);

            if(stringKey == null){
                return null;
            }

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] decode = BaseEncoding.base64().decode(stringKey);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decode);
            PublicKey pub = keyFactory.generatePublic(publicKeySpec);

            return pub;
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
