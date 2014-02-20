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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Encapsulate a PC-style secret
 * 
 * @author Chris Wood
 */
public class HostSecret {
	private static Logger logger = Logger.getLogger(HostSecret.class.getName());
	
	private String description;
	private String username;
	private String password;
	private String email;
	private String note;
	private long timestamp; /* creation or modification timestamp */
	private boolean deleted;
	
	static SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
	
	/**
	 * Default constructor - set creation timestamp
	 */
	public HostSecret() {
		timestamp = System.currentTimeMillis();
	}
	
	/**
	 * Constructor - minimum requirement is the key (description)
	 * @param description 
	 */
	public HostSecret(String description) {
		this();
		this.description = description.trim();
		this.username = "";
		this.password = "";
		this.email = "";
		this.note = "";
	}
	
	/**
	 * Constructor - from provided field contents
	 * @param description
	 * @param username
	 * @param password
	 * @param email
	 * @param note
	 */
	public HostSecret(String description, String username, String password, String email, String note) {
		this(description);
		this.username = username;
		this.password = password;
		this.email = email;
		this.note = note;
	}
	
	/**
	 * Constructor - from provided field contents plus timestamp
	 * @param description
	 * @param username
	 * @param password
	 * @param email
	 * @param note
	 * @param timestampstring
	 */
	public HostSecret(String description, String username, String password, String email, String note, String timestampstring) {
		this(description, username, password, email, note);
		if (timestampstring.length() > 0) {
			setTimestampFromFormatted(timestampstring);
		}
	}
	
	/**
	 * Constructor - from values array
	 * @param values
	 */
	public HostSecret(String[] values) {
		this(values[0], values[1], values[2], values[3], values[4], values[5]);
	}
	
	/**
	 * Constructor - from another secret
	 * @param secret
	 */
	public HostSecret(HostSecret secret) {
		this(secret.getDescription(), secret.getUsername(), secret.getPassword(), secret.getEmail(), secret.getNote(), secret.getFormattedTimestamp());
	}

	/**
	 * Create a secret from a JSON object
	 * @param jsonSecret JSONObject
	 * @return HostSecret
	 * @throws JSONException
	 */
	public static HostSecret fromJSON(JSONObject jsonSecret) throws JSONException {
		HostSecret secret = new HostSecret();
		secret.description = jsonSecret.getString("description").trim();
		secret.username = jsonSecret.getString("username");
		secret.password = jsonSecret.getString("password");
		secret.email = jsonSecret.getString("email");
		secret.note = jsonSecret.getString("note");
		secret.timestamp = jsonSecret.getLong("timestamp");
		secret.deleted = jsonSecret.getBoolean("deleted");
		return secret;
	}

	/**
	 * Create a JSON object from a secret
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject toJSON() throws JSONException {
		JSONObject jsonSecret = new JSONObject();
		jsonSecret.put("description", description);
		jsonSecret.put("username", username);
		jsonSecret.put("password", password);
		jsonSecret.put("email", email);
		jsonSecret.put("note", note);
		jsonSecret.put("timestamp", timestamp);
		jsonSecret.put("deleted", deleted);
		return jsonSecret;
	}

	/**
	 * Compare this secret with another
	 * 
	 * @param secret
	 * @return true if all fields equals, false otherwise
	 */
	public boolean equals(HostSecret secret) {
		return description.equalsIgnoreCase(secret.getDescription()) &&  username.equals(secret.getUsername()) && 
			   password.equals(secret.getPassword()) && 
			   email.equals(secret.getEmail()) && 
			   note.equals(secret.getNote());
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the note
	 */
	public String getNote() {
		return note;
	}

	/**
	 * @param note the note to set
	 */
	public void setNote(String note) {
		this.note = note;
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
	 * @return the timestamp
	 */
	public String getFormattedTimestamp() {
		return sdf.format(timestamp);
	}
	
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestampFromFormatted(String timestamp) {
		try {
			this.timestamp = sdf.parse(timestamp).getTime();
		} catch (ParseException e) {
		   try {
		      this.timestamp = new Date(Long.parseLong(timestamp)).getTime();
         } catch (NumberFormatException e1) {
            logger.log(Level.WARNING, "ParseException setting timestamp: '" + timestamp + "'");
         }
		}
	}

	/**
   * @return the deleted
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * @param deleted the deleted to set
   */
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public String toString() {
		return getDescription();
	}
}
