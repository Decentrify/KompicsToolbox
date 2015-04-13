///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.p2ptoolbox.util.serializer;
//
//import com.google.common.base.Optional;
//import io.netty.buffer.ByteBuf;
//import java.util.HashSet;
//import java.util.Set;
//import org.javatuples.Pair;
//import org.junit.Assert;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.kompics.network.netty.serialization.Serializer;
//import se.sics.kompics.network.netty.serialization.Serializers;
//import se.sics.p2ptoolbox.util.BitBuffer;
//import se.sics.p2ptoolbox.util.network.NatType;
//import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
//import se.sics.p2ptoolbox.util.network.impl.BasicNatedAddress;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class BasicNatedAddressSerializer implements Serializer {
//
//    private static final Logger log = LoggerFactory.getLogger(Serializer.class);
//
//    private final int id;
//
//    public BasicNatedAddressSerializer(int id) {
//        this.id = id;
//    }
//
//    @Override
//    public int identifier() {
//        return id;
//    }
//
//    @Override
//    public void toBinary(Object o, ByteBuf buf) {
//        Serializer basicAddressSerializer = Serializers.lookupSerializer(BasicAddress.class);
//        
//        //basic address
//        BasicNatedAddress obj = (BasicNatedAddress) o;
//        basicAddressSerializer.toBinary(obj.getBaseAddress(), buf);
//
//        //parents
//        Assert.assertTrue("too many parents", obj.getParents().size() < 128);
//        Assert.assertFalse("0 parents", obj.getParents().size() == 0);
//        buf.writeByte(obj.getParents().size());
//        for (BasicAddress adr : (Set<BasicAddress>) obj.getParents()) {
//            basicAddressSerializer.toBinary(adr, buf);
//        }
//        
//        switch(obj.getNatType()) {
//            case NAT :
//                buf.writeByte(0);
//                break;
//            default : 
//                throw new RuntimeException("unknown NatType");
//        }
//    }
//
//    @Override
//    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
//        Serializer basicAddressSerializer = Serializers.lookupSerializer(BasicAddress.class);
//        
//        //basic address
//        BasicAddress basicAddress = (BasicAddress)basicAddressSerializer.fromBinary(buf, Optional.absent());
//        
//        //parents
//        int nrParents = buf.readByte();
//        Set parents = new HashSet<BasicAddress>();
//        while(nrParents > 0) {
//            parents.add(basicAddressSerializer.fromBinary(buf, Optional.absent()));
//            nrParents--;
//        }
//        
//        byte natTypeByte = buf.readByte();
//        NatType natType;
//        switch(natTypeByte) {
//            case 0 :
//                natType = NatType.NAT;
//                break;
//            default : 
//                throw new RuntimeException("unknown NatType");
//        }
//        return new BasicNatedAddress(basicAddress, natType, parents);
//    }
//
//}
