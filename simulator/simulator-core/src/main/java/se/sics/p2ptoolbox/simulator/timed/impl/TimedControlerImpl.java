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
package se.sics.p2ptoolbox.simulator.timed.impl;

import com.google.common.util.concurrent.SettableFuture;
import org.javatuples.Pair;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TimedControlerImpl implements TimedControler {

    private Pair<Component, SettableFuture<Long>> task = null;

    @Override
    public void advance(ComponentDefinition comp, long milis) {
        if (task == null) {
            throw new RuntimeException("timed simulation logic error - no advance expected");
        }
        if (!task.getValue0().id().equals(comp.getComponentCore().id())) {
            String expected = "<" + task.getValue0().getComponent().getClass() + ":" + task.getValue0().id() + ">";
            String found = "<" + comp.getClass() + ":" + comp.getComponentCore().id() + ">";
            throw new RuntimeException("timed simulation logic error - advance from:" + found + " but expected:" + expected);
        }
        task.getValue1().set(milis);
        task = null;
    }

    public SettableFuture<Long> eventTime(Component comp) {
        if (task != null) {
            throw new RuntimeException("timed simulation logic error - ongoing task, tasks should be sequential");
        }
        SettableFuture<Long> taskFuture = SettableFuture.create();
        task = Pair.with(comp, taskFuture);
        return taskFuture;
    }
}
