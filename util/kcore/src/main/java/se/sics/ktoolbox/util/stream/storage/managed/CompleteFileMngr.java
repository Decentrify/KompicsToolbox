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
package se.sics.ktoolbox.util.stream.storage.managed;

import java.util.HashSet;
import java.util.Set;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.stream.FileMngr;
import se.sics.ktoolbox.util.stream.StreamControl;
import se.sics.ktoolbox.util.stream.cache.DelayedRead;
import se.sics.ktoolbox.util.stream.cache.KHint;
import se.sics.ktoolbox.util.stream.ranges.KBlock;
import se.sics.ktoolbox.util.stream.ranges.KRange;
import se.sics.ktoolbox.util.stream.storage.AsyncCompleteStorage;
import se.sics.ktoolbox.util.stream.storage.AsyncOnDemandHashStorage;
import se.sics.ktoolbox.util.stream.util.FileDetails;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CompleteFileMngr implements StreamControl, FileMngr.Reader {

    private final FileDetails fileDetails;
    private final AsyncCompleteStorage file;
    private final AsyncOnDemandHashStorage hash;

    public CompleteFileMngr(FileDetails fileDetails, AsyncCompleteStorage file, AsyncOnDemandHashStorage hash) {
        this.fileDetails = fileDetails;
        this.file = file;
        this.hash = hash;
    }

    //*******************************CONTROL************************************
    @Override
    public void start() {
        file.start();
        hash.start();
    }

    @Override
    public boolean isIdle() {
        return file.isIdle() && hash.isIdle();
    }

    @Override
    public void close() {
        file.close();
        hash.close();
    }

    //*****************************BASIC_READ***********************************
    @Override
    public void read(KRange readRange, DelayedRead delayedResult) {
        file.read(readRange, delayedResult);
    }

    //**************************CACHE_HINT_READ******************************
    @Override
    public void clean(Identifier reader) {
        file.clean(reader);
        hash.clean(reader);
    }

    @Override
    public void setFutureReads(Identifier reader, KHint.Expanded hint) {
        file.setFutureReads(reader, hint);
        hash.setFutureReads(reader, hint);
    }

    //*********************************READER***********************************
    @Override
    public boolean hasBlock(int blockNr) {
        return true;
    }

    @Override
    public boolean hasHash(int blockNr) {
        return true;
    }

    @Override
    public void readHash(KBlock readRange, DelayedRead delayedResult) {
        hash.read(readRange, delayedResult);
    }

    @Override
    public Set<Integer> nextBlocksMissing(int fromBlock, int nrBlocks, Set<Integer> except) {
        return new HashSet<>();
    }

    @Override
    public Set<Integer> nextHashesMissing(int fromBlock, int nrBlocks, Set<Integer> except) {
        return new HashSet<>();
    }
}
