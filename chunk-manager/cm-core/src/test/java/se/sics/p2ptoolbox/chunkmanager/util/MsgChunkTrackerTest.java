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

package se.sics.p2ptoolbox.chunkmanager.util;

import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MsgChunkTrackerTest {
    @Test
    public void test() {
        IncompleteChunkTracker mct = new IncompleteChunkTracker(4);
        mct.add(new Chunk(UUID.randomUUID(), 0, 4, new byte[]{1}));
        mct.add(new Chunk(UUID.randomUUID(), 3, 4, new byte[]{1,2}));
        mct.add(new Chunk(UUID.randomUUID(), 1, 4, new byte[]{2}));
        mct.add(new Chunk(UUID.randomUUID(), 4, 4, new byte[]{1,3,4}));
        mct.add(new Chunk(UUID.randomUUID(), 2, 4, new byte[]{1}));
        
        byte[] msg = mct.getMsg();
        Assert.assertEquals(8, msg.length);
    }
}
