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
package se.sics.ktoolbox.util.stream.util;

import java.util.List;
import org.javatuples.Pair;
import se.sics.ktoolbox.util.stream.StreamEndpoint;
import se.sics.ktoolbox.util.stream.StreamResource;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FileDetails {
    public final long length;
    public final Pair<StreamEndpoint, StreamResource> mainResource;
    public final List<Pair<StreamEndpoint, StreamResource>> secondaryResources;
    public final int nrBlocks;
    public final BlockDetails defaultBlock;
    public final BlockDetails lastBlock;
    public final String hashAlg;
    
    public FileDetails(long length, Pair<StreamEndpoint, StreamResource> mainResource, List<Pair<StreamEndpoint, StreamResource>> secondaryResources, 
            int nrBlocks, BlockDetails defaultBlock, BlockDetails lastBlock, String hashAlg) {
        this.length = length;
        this.mainResource = mainResource;
        this.secondaryResources = secondaryResources;
        this.nrBlocks = nrBlocks;
        this.defaultBlock = defaultBlock;
        this.lastBlock = lastBlock;
        this.hashAlg = hashAlg;
    }
    
    public BlockDetails getBlockDetails(int blockNr) {
        if(blockNr == nrBlocks - 1) {
            return lastBlock;
        } else {
            return defaultBlock;
        }
    }
}
