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
package se.sics.ktoolbox.util.managedStore.core;

import com.google.common.base.Optional;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import se.sics.ktoolbox.util.managedStore.core.impl.util.PrepDwnlInfo;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public interface TransferMngr {
    
    public void tearDown();

    public Optional<Set<Integer>> downloadHash(int nrHashes);

    public Optional<Integer> downloadData();

    public void writeHashes(Map<Integer, ByteBuffer> hashes, Set<Integer> missingHashes);

    public void writePiece(int pieceNr, ByteBuffer piece);

    public void resetPiece(int pieceNr);

    public int contiguousBlocks(int fromBlockNr);

    public boolean isComplete();

    public double percentageComplete();

    /**
     * Should probably make these as configurable - instead of externally
     * activated
     */
    public void checkCompleteBlocks();

    public int prepareDownload(int targetBlockNr, PrepDwnlInfo prepInfo);

}
