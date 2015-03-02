package se.sics.p2ptoolbox.gradient.net;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.gradient.api.util.GradientPeerView;
import se.sics.p2ptoolbox.gradient.core.GradientShuffle;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializer for the Gradient Shuffle Wrapper.
 *
 * Created by babbarshaer on 2015-03-01.
 */
public class GradientShuffleSerializer implements Serializer<GradientShuffle> {
    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, GradientShuffle obj) throws SerializerException, SerializationContext.MissingException {
        
        List<GradientPeerView> exchangeNodes = obj.getGradientExchangeNodes();
        if(exchangeNodes.isEmpty()){
            buf.writeInt(-1);
            return buf;
        }
        
        Serializer serializer = context.getSerializer(exchangeNodes.get(0).getClass());
        buf.writeInt(exchangeNodes.size());
        for(GradientPeerView gpv : exchangeNodes){
            serializer.encode(context, buf, gpv);
        }
        
        return buf;
    }

    @Override
    public GradientShuffle decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
        
        Serializer serializer = context.getSerializer(GradientPeerView.class);
        int listSize = buf.readInt();
        
        List<GradientPeerView> exchangeNodes = new ArrayList<GradientPeerView>();
        if(listSize == -1){
            return new GradientShuffle(exchangeNodes);
        }
        
        for(int i =0; i < listSize ; i++){
            exchangeNodes.add((GradientPeerView) serializer.decode(context, buf));
        }

        return new GradientShuffle(exchangeNodes);
    }

    @Override
    public int getSize(SerializationContext context, GradientShuffle obj) throws SerializerException, SerializationContext.MissingException {
        
        List<GradientPeerView> gradientPeerViewList = obj.getGradientExchangeNodes();
        
        int size=0;
        if(gradientPeerViewList.isEmpty()){
            
            size += (Integer.SIZE / 8);
            return size;
        }
        
        Serializer serializer  = context.getSerializer(gradientPeerViewList.get(0).getClass());
        for(GradientPeerView gpv : gradientPeerViewList){
            serializer.getSize(context, gpv);
        }
        
        return size;
    }
}
