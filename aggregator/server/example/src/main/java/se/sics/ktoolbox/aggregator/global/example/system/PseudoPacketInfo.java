/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.ktoolbox.aggregator.global.example.system;

import se.sics.ktoolbox.aggregator.util.PacketInfo;

/**
 * Pseudo Packet Information.
 *  
 * Created by babbarshaer on 2015-09-05.
 */
public class PseudoPacketInfo implements PacketInfo {
    
    private float response;
    private float price;
    
    public PseudoPacketInfo(float response, float price){
        
        this.response = response;
        this.price = price;
    }
    
    public float getResponse(){
        return this.response;
    }
    
    public float getPrice(){
        return this.price;
    }
    
    @Override
    public String toString() {
        return "PseudoPacketInfo{" +
                "response=" + response +
                ", price=" + price +
                '}';
    }
}
