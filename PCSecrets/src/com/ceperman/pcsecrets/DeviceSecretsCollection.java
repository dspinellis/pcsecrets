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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Encapsulates a secrets collection received from a device.
 * 
 * Note that JSON data for this collection does not carry a last sync date. This is
 * obtained from the sync device collection.
 * 
 * The SyncDevice object holds device info.
 * 
 * @author Chris Wood
 */
@SuppressWarnings("serial")
public class DeviceSecretsCollection extends EncryptableSecretsCollection {
  private static final Logger logger = Logger.getLogger(MainWindow.class.getName());
  
  private String id; // device id
  private String ip; // source IP address
  
  /**
   * Default constructor
   */
  public DeviceSecretsCollection() {}
  
  /**
   * Copy constructor
   * 
   * @param other 
   */
  public DeviceSecretsCollection(DeviceSecretsCollection other) {
    super(other);
    this.id = other.id;
    this.ip = other.ip;
  }

  /* Extract the device info.
   * 
   * @see com.ceperman.pcsecrets.EncryptableSecretsCollection#fromJSON(org.json.JSONObject)
   */
  @Override
  protected void fromJSON(JSONObject jsonValues) throws JSONException {
    super.fromJSON(jsonValues);
    
    if (jsonValues.has(SyncDevice.ID)) {
      id = jsonValues.getString(SyncDevice.ID);
      logger.log(Level.FINE, "Device id: " + id);
    } else {
      logger.log(Level.WARNING, "No device id present");
    }
    ip = getSourceName();
  }

  /**
   * Get the device id
   * @return id
   */
  public String getDeviceId() {
    return id;
  }

  /**
   * @return the ip
   */
  public String getIp() {
    return ip;
  }
  
}