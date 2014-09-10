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
package se.sics.p2ptoolbox.croupier.core.net;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.croupier.api.CroupierMsg;
import se.sics.p2ptoolbox.croupier.api.net.CroupierContext;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface CroupierAdapter<E extends CroupierMsg.Base> {

    public E decode(CroupierContext context, ByteBuf buffer);

    public ByteBuf encode(CroupierContext context, E object, ByteBuf buffer);

    public int getEncodedSize(CroupierContext context, E object);

    public static interface Request<E extends CroupierMsg.Request> extends CroupierAdapter<E> {

    }

    public static interface Response<E extends CroupierMsg.Response> extends CroupierAdapter<E> {

    }
}
