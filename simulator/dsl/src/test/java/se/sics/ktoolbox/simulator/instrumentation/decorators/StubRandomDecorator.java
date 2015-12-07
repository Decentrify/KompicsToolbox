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
package se.sics.ktoolbox.simulator.instrumentation.decorators;

import se.sics.ktoolbox.simulator.instrumentation.KDecorator;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class StubRandomDecorator implements KDecorator {
    public int beforeNoArgConstructor = 0;
    public int beforeSeedConstructor = 0;
    
    @Override
    public void applyBefore(String callingClass, Object[] arg) {
        if(arg.length == 0) {
            System.out.println("intercepted no arg random constructor - replaced with seed:0");
            beforeNoArgConstructor++; 
        } else if(arg.length == 1 && (arg[0] instanceof Long)){
            beforeSeedConstructor++;
            System.out.println("intercepted random constructor with seed:" + arg[0]);
        }
    }

    @Override
    public void applyAfter(String callingClass, Object[] arg) {
    }
}
