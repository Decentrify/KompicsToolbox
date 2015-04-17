/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.p2ptoolbox.chunkmanager.util;

import java.util.UUID;
import se.sics.p2ptoolbox.util.traits.Trait;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface Chunked extends Trait {
    public UUID getMessageId();
    public int getChunkNr();
    public int getLastChunk();
}
