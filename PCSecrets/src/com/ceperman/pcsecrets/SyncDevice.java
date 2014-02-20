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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Encapsulated the deletions information to/from a device.
 * 
 * @author Chris Wood
 */
public class SyncDevice {
  private static Logger logger = Logger.getLogger(SyncDevice.class.getName());
  @SuppressWarnings("javadoc")
  public static final String ID = "id";
  
  private static final String UNKNOWN = "unknown";
  private static final String NAME = "name";
  private static final String TIMESTAMP = "timestamp";
  private static final String LASTIP = "lastip";
  private String id = UNKNOWN;
  private String displayName = UNKNOWN;
  private String lastIP;
  private long syncTimestamp;

  /* The map key is the secret description and the value is the timestamp */
  private Map<String, DeletedSecret> deletedSecrets = new HashMap<String, DeletedSecret>();
  
  /**
   * Default constructor
   */
  public SyncDevice() {
  }
  
  /**
   * Constructor for id only
   * 
   * @param id the device id
   */
  public SyncDevice(String id) {
    this.id = id;
    this.displayName = id;
  }
  
  /**
   * Constructor from JSONObject
   * 
   * "name" is only meaningful for a locally stored object.
   * 
   * @param jo a JSONObject value
   */
  public SyncDevice(JSONObject jo) {
    try {
      if (jo.has(ID)) {
        id = jo.getString(ID);
      } else {
        id = UNKNOWN;
        logger.log(Level.FINE, "no id found");
      }
      if (jo.has(NAME)) {
        displayName = jo.getString(NAME);
      } else {
        logger.log(Level.FINE, "no name found");
      }
      if (jo.has(TIMESTAMP)) {
        syncTimestamp = jo.getLong(TIMESTAMP);
      } else {
        logger.log(Level.FINE, "no timestamp found");
      }
      if (jo.has(LASTIP)) {
        lastIP = jo.getString(LASTIP);
      } else {
        logger.log(Level.FINE, "no timestamp found");
      }
      JSONArray records = jo.getJSONArray("ds");
      for (int i = 0; i < records.length(); i++) {
        JSONObject jsonObject = records.getJSONObject(i);
        DeletedSecret deletedSecret = new DeletedSecret(jsonObject);
        deletedSecrets.put(deletedSecret.getDescription(), deletedSecret);
      }
      logger.log(Level.FINE, "deleted records: " + records.length());
    } catch (JSONException e) {
      e.printStackTrace();
      throw new RuntimeException("JSONException - program error");
    }
  }
  
  /**
   * Perform deep copy. This creates new objects for all collections and
   * all contained objects.
   * @return new copy
   */
  public SyncDevice deepcopy() {
    SyncDevice copy = new SyncDevice(this.id);
    copy.displayName = this.displayName;
    copy.syncTimestamp = this.syncTimestamp;
    copy.lastIP = this.lastIP;
    copy.deletedSecrets = new HashMap<String, DeletedSecret>();
    for (Entry<String, DeletedSecret> deletedSecret : this.deletedSecrets.entrySet()) {
      copy.deletedSecrets.put(deletedSecret.getKey(), 
              new DeletedSecret(deletedSecret.getValue().getDescription(), 
                                deletedSecret.getValue().getTimestamp()));
    }
    return copy;
  }
  
  /**
   * Add a deleted secret
   * 
   * @param key
   * @param timestamp
   */
  public void addSecret(String key, Long timestamp) {
    deletedSecrets.put(key, new DeletedSecret(key, timestamp));
  }
  
  /**
   * Remove a deleted secret
   * 
   * @param key
   */
  public void removeSecret(String key) {
    deletedSecrets.remove(key);
  }
  
  /**
   * Clear all deleted secrets
   * 
   * @param key
   */
  public void clearDeletedSecrets() {
    deletedSecrets.clear();
  }
  
  /**
   * Return the deleted secrets
   * 
   * @return deleted secrets
   */
  public Collection<DeletedSecret> getDeletedSecrets() {
    return deletedSecrets.values();
  }
  
  /**
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * @param name the display name to set
   */
  public void setDisplayName(String name) {
    this.displayName = name;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }
  
  /**
   * @return the syncTimestamp
   */
  public long getSyncTimestamp() {
    return syncTimestamp;
  }

  /**
   * @param syncTimestamp the syncTimestamp to set
   */
  public void setSyncTimestamp(long syncTimestamp) {
    this.syncTimestamp = syncTimestamp;
  }

  /**
   * @return the lastIP
   */
  public String getLastIP() {
    return lastIP;
  }

  /**
   * @param lastIP the lastIP to set
   */
  public void setLastIP(String lastIP) {
    this.lastIP = lastIP;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (displayName.length() > 0) {
      return displayName;
    }
    return id;
  }

  /**
   * Convert this object into a JSONObject
   * @return JSONObject
   * @throws JSONException 
   */
  public JSONObject toJSON() throws JSONException {
    JSONObject jo = new JSONObject();
    jo.put(ID, id);
    jo.put(NAME, displayName);
    jo.put(TIMESTAMP, syncTimestamp);
    jo.put(LASTIP, lastIP);
    JSONArray records = new JSONArray();
    Collection<DeletedSecret> deleted = deletedSecrets.values();
    for (DeletedSecret deletedSecret : deleted) {
      records.put(deletedSecret.toJSON());
    }
    jo.put("ds", records);
    return jo;
  }

  /**
   * Represents a deleted secret
   * 
   * @author Chris Wood
   */
  public class DeletedSecret {
    private String description;
    private long timestamp;
    
    /**
     * Constructor from individual values
     * 
     * @param description
     * @param timestamp
     */
    public DeletedSecret(String description, long timestamp) {
      this.setDescription(description);
      this.setTimestamp(timestamp);
    }
    
    /**
     * Constructor from HostSecret
     * 
     * @param secret
     */
    public DeletedSecret(HostSecret secret) {
      this.setDescription(secret.getDescription());
      this.setTimestamp(secret.getTimestamp());
    }
    
    /**
     * Constructor from JSON Array
     * @param jsonObject 
     * @throws JSONException 
     */
    public DeletedSecret(JSONObject jsonObject) throws JSONException {
      description = jsonObject.getString("d");
      timestamp = jsonObject.getLong("t");
    }

    /**
     * @return the description
     */
    public String getDescription() {
      return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
      this.description = description;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
      return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }
    
    /**
     * Convert to JSON Object
     * @return JSONObject
     * @throws JSONException 
     */
    private JSONObject toJSON() throws JSONException {
      JSONObject jo = new JSONObject();
      jo.put("d", description);
      jo.put("t", timestamp);
      return jo;
    }
    
  }
}
