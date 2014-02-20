/**
 * Copyright 2013 Chris Wood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceperman.pcsecrets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Collection of known sync devices
 * 
 * @author Chris Wood
 */
@SuppressWarnings("serial")
public class SyncDeviceCollection extends HashMap<String, SyncDevice> implements ListModel<SyncDevice> {
//  private static Logger logger = Logger.getLogger(SyncDeviceCollection.class.getName());
  
  private Collection<ListDataListener> dataListeners = new ArrayList<ListDataListener>();
  private boolean changed;
  
  /**
   * Add a new sync device
   * @param sd
   */
  public void add(SyncDevice sd) {
     put(sd.getId(), sd);
  }

  /**
   * Add a deleted record to all sync devices
   * 
   * @param description
   */
  public void addDeleted(String description) {
     long timestamp = System.currentTimeMillis();
     Collection<SyncDevice> syncDevices = values();
     for (SyncDevice syncDevice : syncDevices) {
        syncDevice.addSecret(description, timestamp);
     }
     contentsChanged();
  }

  /**
   * Notify listeners that things have changed
   */
  public void contentsChanged() {
     for (ListDataListener listener : dataListeners) {
        listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, size()));
     }
  }

  /**
   * Notify listeners that something was added
   * @param index 
   */
  public void intervalAdded(int index) {
     for (ListDataListener listener : dataListeners) {
        listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index));
     }
  }

  /**
   * Notify listeners that something was removed
   * @param index 
   */
  public void intervalRemoved(int index) {
     for (ListDataListener listener : dataListeners) {
        listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index));
     }
  }

  /**
   * Construct a stringified JSON array of the current device collection
   * 
   * @return a stringified JSONArray
   * @throws JSONException 
   */
  public String toJSON() throws JSONException {
     JSONArray jsonDevices = new JSONArray();
     Collection<SyncDevice> syncDevices = values();
     for (SyncDevice syncDevice : syncDevices) {
        jsonDevices.put(syncDevice.toJSON());
     }
     return jsonDevices.toString();
  }

  /**
   * Load the device collection from a stringified JSON array
   * 
   * @param stringArray
   * @throws JSONException 
   */
  public void fromJSON(String stringArray) throws JSONException {
     JSONArray jsonArray = (JSONArray) new JSONTokener(stringArray).nextValue();
     for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jo = jsonArray.getJSONObject(i);
        SyncDevice syncDevice = new SyncDevice(jo);
        put(syncDevice.getId(), syncDevice);
     }
  }

  @Override
  public void addListDataListener(ListDataListener l) {
     dataListeners.add(l);
  }

  @Override
  public SyncDevice getElementAt(int index) {
     if (size() < index +1) return null;
     return (SyncDevice)values().toArray()[index];
  }

  @Override
  public int getSize() {
     return size();
  }

  @Override
  public void removeListDataListener(ListDataListener l) {
     dataListeners.remove(l);
  }

/**
 * Test changed indicator
 * @return the changed
 */
public boolean isChanged() {
   return changed;
}

/**
 * Set changed indicator
 * @param changed the changed to set
 */
public void setChanged(boolean changed) {
   this.changed = changed;
}
}
