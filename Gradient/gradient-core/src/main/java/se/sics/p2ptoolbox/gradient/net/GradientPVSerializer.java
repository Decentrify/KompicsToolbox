package se.sics.p2ptoolbox.gradient.net;

import io.netty.buffer.ByteBuf;
import org.javatuples.Pair;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.gradient.api.util.GradientPeerView;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * Serializer for the descriptor wrapper.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientPVSerializer implements Serializer<GradientPeerView> {
    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, GradientPeerView obj)throws SerializerException, SerializationContext.MissingException {
        Pair<Byte,Byte> pvCode = context.getCode(obj.peerView.getClass());
        buf.writeByte(pvCode.getValue0());
        buf.writeByte(pvCode.getValue1());
        
        Serializer serializer = context.getSerializer(obj.peerView.getClass());
        serializer.encode(context, buf, obj.peerView);
        context.getSerializer(VodAddress.class).encode(context, buf, obj.src);
        buf.writeInt(obj.getAge());
        
        return buf;
    }

    @Override
    public GradientPeerView decode(SerializationContext context, ByteBuf buf)throws SerializerException, SerializationContext.MissingException {
        
        Byte pvCode0 = buf.readByte();
        Byte pvCode1 = buf.readByte();
        
        Serializer pvs = context.getSerializer(PeerView.class, pvCode0, pvCode1);
        PeerView peerView = (PeerView)pvs.decode(context, buf);
        VodAddress src = context.getSerializer(VodAddress.class).decode(context, buf);
        int age = buf.readInt();
        
        return new GradientPeerView(peerView, src, age);
    }

    @Override
    public int getSize(SerializationContext context, GradientPeerView obj) throws SerializerException, SerializationContext.MissingException {

        int size=0;
        size += 2 * Byte.SIZE / 8;
        Serializer pvS = context.getSerializer(obj.peerView.getClass());
        size += pvS.getSize(context, obj.peerView);
        size += context.getSerializer(VodAddress.class).getSize(context, obj.src);
        size += Integer.SIZE /8; //age
        
        return size;
    }
}
