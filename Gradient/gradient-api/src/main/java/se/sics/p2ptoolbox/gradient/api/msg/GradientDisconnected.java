/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.gradient.api.msg;

import java.util.UUID;

/**
 * Indication Event sent by gradient in case it could not
 * find any node to shuffle with.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientDisconnected  extends GradientMsg.OneWay{
    
    public final int overlayId;
    
    public GradientDisconnected(int overlayId) {
        super();
        this.overlayId = overlayId;
    }
}
