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

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a collection of secrets with a sync/last updated timestamp
 * 
 * @author Chris Wood
 */
@SuppressWarnings("serial")
public class DatedSecretsCollection extends SecretsCollection {

	private static Logger logger = Logger.getLogger(StoredSecretsCollection.class.getName());
	private long lastSyncTimestamp;
	
	/**
	 * Default constructor
	 */
	public DatedSecretsCollection() {}
  
  /**
   * Constructor from DatedSecretsCollection
   * @param other 
   */
  public DatedSecretsCollection(DatedSecretsCollection other) {
    super(other);
    this.lastSyncTimestamp = other.lastSyncTimestamp;
  }

	/**
	 * @return the lastSyncTimestamp
	 */
	public long getLastSyncTimestamp() {
		return lastSyncTimestamp;
	}

	/**
	 * @param lastSyncTimestamp the lastSyncTimestamp to set
	 */
	public void setLastSyncTimestamp(long lastSyncTimestamp) {
		this.lastSyncTimestamp = lastSyncTimestamp;
	}

	/**
	 * @return the timestamp
	 */
	public String getFormattedSyncDate() {
		return HostSecret.sdf.format(lastSyncTimestamp);
	}

	/**
	 * @param timestampstring the timestamp to set
	 */
	public void setFormattedSyncDate(String timestampstring) {
		try {
			lastSyncTimestamp = HostSecret.sdf.parse(timestampstring).getTime();
		} catch (ParseException e) {
			logger.log(Level.WARNING, "setFormattedSyncDate: " + e.getLocalizedMessage());
		}
	}

}