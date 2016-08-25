///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
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
//package se.sics.ktoolbox.util.managedStore.core.impl;
//
//import com.google.common.base.Optional;
//import java.nio.ByteBuffer;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeMap;
//import java.util.TreeSet;
//import org.javatuples.Pair;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.ktoolbox.util.identifiable.Identifier;
//import se.sics.ktoolbox.util.managedStore.core.FileMngr;
//import se.sics.ktoolbox.util.managedStore.core.HashMngr;
//import se.sics.ktoolbox.util.managedStore.core.impl.util.PrepDwnlInfo;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class TransferHelper {
//
//    private static final Logger LOG = LoggerFactory.getLogger(TransferHelper.class);
//
//    public static Pair<Set<Integer>, Set<Integer>> prepareTransferReq(String logPrefix, SimpleTransferMngr destination, 
//            int pieces, PrepDwnlInfo prepInfo) {
//        Set<Integer> requestHashes = prepareHashesReq(destination, prepInfo);
//        Set<Integer> requestPieces = preparePieceReq(destination, pieces);
//        return Pair.with(requestHashes, requestPieces);
//    }
//
//    private static Set<Integer> prepareHashesReq(SimpleTransferMngr destination, PrepDwnlInfo prepInfo) {
//        Set<Integer> preparedHashes = new TreeSet<>();
//        while (true) {
//            Optional<Set<Integer>> nextHashes = destination.downloadHash(prepInfo.hashesPerMsg);
//            if (nextHashes.isPresent()) {
//                preparedHashes.addAll(nextHashes.get());
//            } else {
//                break;
//            }
//        }
//
//        return preparedHashes;
//    }
//
//    private static Set<Integer> preparePieceReq(SimpleTransferMngr destination, int nrOfMsgs) {
//        Set<Integer> preparedPieces = new TreeSet<>();
//        for (int i = 0; i < nrOfMsgs; i++) {
//            Optional<Integer> nextPiece = destination.downloadData();
//            if (nextPiece.isPresent()) {
//                preparedPieces.add(nextPiece.get());
//            } else {
//                break;
//            }
//        }
//        return preparedPieces;
//    }
//
//    public static Pair<Map<Integer, ByteBuffer>, Set<Integer>> prepareHashesResp(String logPrefix, HashMngr source, Set<Integer> hashes) {
//        Map<Integer, ByteBuffer> successHashes = new TreeMap<>();
//        Set<Integer> missingHashes = new TreeSet<>();
//        for (Integer hashNr : hashes) {
//            if (source.hasHash(hashNr)) {
//                successHashes.put(hashNr, source.readHash(hashNr));
//            } else {
//                missingHashes.add(hashNr);
//            }
//        }
//        return Pair.with(successHashes, missingHashes);
//    }
//
//    public static Pair<Map<Integer, ByteBuffer>, Set<Integer>> preparePiecesResp(Identifier readerId, Set<Integer> bufferBlocks, String logPrefix, FileMngr source, Set<Integer> pieces) {
//        Map<Integer, ByteBuffer> successPieces = new TreeMap<>();
//        Set<Integer> missingPieces = new TreeSet<>();
//        for (Integer pieceNr : pieces) {
//            if (source.hasPiece(pieceNr)) {
//                ByteBuffer value = source.readPiece(readerId, pieceNr, bufferBlocks);
//                successPieces.put(pieceNr, value);
//            } else {
//                missingPieces.add(pieceNr);
//            }
//        }
//        return Pair.with(successPieces, missingPieces);
//    }
//
//    public static void write(String logPrefix, SimpleTransferMngr destination, 
//            Pair<Map<Integer, ByteBuffer>, Set<Integer>> hash, Pair<Map<Integer, ByteBuffer>, Set<Integer>> pieces) {
//        destination.writeHashes(hash.getValue0(), hash.getValue1());
//
//        for (Map.Entry<Integer, ByteBuffer> p : pieces.getValue0().entrySet()) {
//            destination.writePiece(p.getKey(), p.getValue());
//        }
//        for (Integer p : pieces.getValue1()) {
//            destination.resetPiece(p);
//        }
//    }
//}
