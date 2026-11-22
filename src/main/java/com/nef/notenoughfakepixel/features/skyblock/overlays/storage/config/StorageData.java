/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.overlays.storage.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StorageData {
    public HashMap<String, List<StorageContainer>> containers = new HashMap<>();

    public StorageContainer getContainer(String type,int containerNum){
        if(containers.containsKey(type)){
            if(!containers.get(type).isEmpty()){
                for(StorageContainer c : containers.get(type)){
                    if(c.containerNumber == containerNum) return c;
                }
            }
        }
        return null;
    }
    public void addContainer(String type,StorageContainer c){
        containers.computeIfAbsent(type, k -> new ArrayList<>()).add(c);
    }
}
