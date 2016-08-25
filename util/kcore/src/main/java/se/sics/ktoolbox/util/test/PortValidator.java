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
package se.sics.ktoolbox.util.test;

import se.sics.kompics.PortType;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PortValidator implements Validator {

    private final boolean expectedPositive;
    private final Class<? extends PortType> expectedPortType;
    private boolean foundPositive;
    private Class<? extends PortType> foundPortType;

    public PortValidator(Class<? extends PortType> expectedPortType, boolean expectedPositive) {
        this.expectedPortType = expectedPortType;
        this.expectedPositive = expectedPositive;
    }

    public void setFound(boolean foundPositive, Class<? extends PortType> foundPortType) {
        this.foundPortType = foundPortType;
        this.foundPositive = foundPositive;
    }

    @Override
    public boolean isValid() {
        if(expectedPositive != foundPositive) {
            return false;
        }
        return expectedPortType.equals(foundPortType);
    }
    
    @Override
    public String toString() {
        return "PortValidator";
    }
}
