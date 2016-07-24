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
package se.sics.ktoolbox.util.stream.transfer;

import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.stream.StreamControl;
import se.sics.ktoolbox.util.stream.TransferMngr;
import se.sics.ktoolbox.util.stream.cache.DelayedRead;
import se.sics.ktoolbox.util.stream.cache.KHint;
import se.sics.ktoolbox.util.stream.ranges.KBlock;
import se.sics.ktoolbox.util.stream.storage.managed.CompleteFileMngr;
import se.sics.ktoolbox.util.stream.util.FileDetails;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class UploadTransferMngr implements StreamControl, TransferMngr.Reader {

    private final FileDetails fileDetails;
    private final CompleteFileMngr file;

    public UploadTransferMngr(FileDetails fileDetails, CompleteFileMngr file) {
        this.fileDetails = fileDetails;
        this.file = file;
    }

    //********************************CONTROL***********************************
    @Override
    public void start() {
        file.start();
    }

    @Override
    public boolean isIdle() {
        return file.isIdle();
    }

    @Override
    public void close() {
        file.close();
    }

    //*****************************CACHE_HINT_READ******************************
    @Override
    public void clean(Identifier reader) {
        file.clean(reader);
    }

    @Override
    public void setFutureReads(Identifier reader, KHint.Expanded hint) {
        file.setFutureReads(reader, hint);
    }

    //***********************************READER*********************************
    @Override
    public boolean hasBlock(int blockNr) {
        return true;
    }

    @Override
    public boolean hasHash(int blockNr) {
        return true;
    }

    @Override
    public void readHash(int blockNr, DelayedRead delayedResult) {
        KBlock hashRange = BlockHelper.getHashRange(blockNr, fileDetails);
        file.readHash(hashRange, delayedResult);
    }

    @Override
    public void readBlock(int blockNr, DelayedRead delayedResult) {
        KBlock blockRange = BlockHelper.getBlockRange(blockNr, fileDetails);
        file.read(blockRange, delayedResult);
    }
}
