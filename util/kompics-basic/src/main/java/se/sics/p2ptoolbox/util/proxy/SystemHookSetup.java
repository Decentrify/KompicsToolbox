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

package se.sics.p2ptoolbox.util.proxy;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SystemHookSetup {
    private final Map<String, Hook.Definition> hooks = new HashMap<>();
 
    public void register(String hookName, Hook.Definition hook) {
        if(hooks.containsKey(hookName)) {
            throw new RuntimeException("double hook:" + hookName + " definition - logic error");
        }
        hooks.put(hookName, hook);
    }
    
    public <HR extends Hook.Required> Optional<String> missingHook(HR[] requiredHooks) {
        for(HR requiredHook : requiredHooks) {
            if(!hooks.containsKey(requiredHook.toString())) {
                return Optional.of(requiredHook.toString());
            }
        }
        return Optional.absent();
    }
    
    public <H extends Hook.Definition> H getHook(String hookName, Class<H> hookClass) {
        Hook.Definition hook = hooks.get(hookName);
        if(hook == null) {
            throw new RuntimeException("logic error - hook:" + hookName + " not defined");
        }
        if(hookClass.isAssignableFrom(hook.getClass())) {
            throw new RuntimeException("logic error - hook:" + hookName + " bad class, found:" + hook.getClass() + " expected:" + hookClass);
        }
        return (H)hook;
    }
}
