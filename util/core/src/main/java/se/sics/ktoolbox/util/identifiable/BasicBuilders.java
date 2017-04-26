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
package se.sics.ktoolbox.util.identifiable;

import java.util.UUID;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicBuilders {

    public static class IntBuilder implements IdentifierBuilder {

        public final int base;

        public IntBuilder(int base) {
            this.base = base;
        }
    }

    public static class StringBuilder implements IdentifierBuilder {

        public final String base;

        public StringBuilder(String base) {
            this.base = base;
        }
    }

    public static class ByteBuilder implements IdentifierBuilder {

        public final byte[] base;

        public ByteBuilder(byte[] base) {
            this.base = base;
        }
    }
    
    public static class UUIDBuilder implements IdentifierBuilder {
        public final UUID base;
        
        public UUIDBuilder(UUID base) {
            this.base = base;
        }
    }
}
