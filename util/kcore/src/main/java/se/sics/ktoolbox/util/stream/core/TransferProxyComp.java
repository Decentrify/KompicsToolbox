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
package se.sics.ktoolbox.util.stream.core;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.result.DelayedExceptionSyncHandler;
import se.sics.ktoolbox.util.stream.util.FileDetails;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TransferProxyComp {

    private final static Logger LOG = LoggerFactory.getLogger(TransferProxyComp.class);
    private String logPrefix = "";
    
    private final ComponentProxy proxy;
    private final DelayedExceptionSyncHandler exSyncHandler = null; //TODO init
    private final MultiFileTracker tracker;
    
    public TransferProxyComp(Config config, ComponentProxy proxy, Map<Integer, FileDetails> transferDetails, boolean upload) {
        this.proxy = proxy;
        tracker = new MultiFileTracker(config, proxy, exSyncHandler, transferDetails, upload);
    }
    
//    public boolean writePiece(KPiece writeRange, byte[] val) {
//        boolean completeBlock = transferMngr.writePiece(writeRange, val);
//        if(completeBlock) {
//            int blockNr = writeRange.parentBlock();
//            KBlock blockRange = BlockHelper.getBlockRange(blockNr, fileDetails);
//            byte[] blockBytes = transferMngr.getBlock(blockNr);
//            KReference<byte[]> blockRef = KReferenceFactory.getReference(blockBytes);
//            //TODO Alex - add keeping track of when write finish
//            storage.write(blockRange, blockRef, new NopDelayedWrite());
//        }
//        return completeBlock;
//    }
    
//    public boolean moreWork() {
//        return transferMngr.moreWork();
//    }
//    
//    @Override
//    public boolean pendingWork() {
//        return transferMngr.pendingWork();
//    }
}
