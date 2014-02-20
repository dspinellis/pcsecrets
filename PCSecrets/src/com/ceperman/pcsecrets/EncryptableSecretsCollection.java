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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ceperman.pcsecrets.SecurityUtils.CipherInfo;
import com.ceperman.utils.ExpandableByteBuffer;

/**
 * Represents an encrypted collection of secrets. Includes methods to read
 * and write encrypted streams.
 * 
 * @author chris
 */
@SuppressWarnings("serial")
public class EncryptableSecretsCollection extends DatedSecretsCollection {
	private static Logger logger = Logger.getLogger(EncryptableSecretsCollection.class.getName());
	
	private CipherInfo cipherInfo;
	private String sourceName;
	private byte[] pswdBytes;
	
	/**
   * Default constructor
   */
  public EncryptableSecretsCollection() {}
	
	/**
   * Copy constructor
   * 
   * @param other 
   */
   public EncryptableSecretsCollection(EncryptableSecretsCollection other) {
      super(other);
      this.sourceName = other.sourceName;
      this.cipherInfo = other.cipherInfo;
      this.pswdBytes = other.pswdBytes;
   }

  /** 
   * Copy attributes from another collection
	 * @param source source collection
   */
   public void copyAttributesFrom(EncryptableSecretsCollection source) {
      cipherInfo = source.cipherInfo;
      sourceName = source.sourceName;
      pswdBytes = source.pswdBytes;
   }

	/**
	 * Load secrets from encrypted JSON input stream. The stream must be positioned at the first
	 * encrypted data byte.
	 * 
	 * @param is InputStream
	 * @return secrets collection size
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public int loadSecretsFromEncryptedJSONStream(InputStream is) throws IOException, JSONException, IllegalBlockSizeException, BadPaddingException {
		JSONObject jsonValues = decryptJSONObject(is);
		fromJSON(jsonValues);
		return getSize();
	}

  /**
   * Extract the secrets from a JSON object. 
   * Overridable if derived classes need to extract more values from the JSON object.
   * @param jsonValues
   * @throws JSONException
   */
   protected void fromJSON(JSONObject jsonValues) throws JSONException {
      /* get the last sync date */
      if (jsonValues.has("syncdate")) {
         setLastSyncTimestamp(jsonValues.getLong("syncdate"));
         logger.log(Level.FINE, "Retrieved syncDate: " + getFormattedSyncDate());
      }
      /* get the secrets */
      JSONArray jsonSecrets = jsonValues.getJSONArray("secrets");
      for (int i = 0; i < jsonSecrets.length(); i++) {
         addOrUpdate(HostSecret.fromJSON(jsonSecrets.getJSONObject(i)));
      }
      logger.log(Level.FINE, "Retrieved secrets: " + jsonSecrets.length());
   }

  /**
   * Decrypt the JSON object from the stream
   * 
   * @param is input stream
   * @return JSONObject
   * @throws IllegalBlockSizeException
   * @throws BadPaddingException
   * @throws JSONException
   * @throws UnsupportedEncodingException
   */
   private JSONObject decryptJSONObject(InputStream is) throws IllegalBlockSizeException, BadPaddingException,
               JSONException, UnsupportedEncodingException {
      byte[] bytes = readAllBytesFromStream(is);
      byte[] decryptedBytes = cipherInfo.decryptCipher.doFinal(bytes);
      JSONObject jsonValues = new JSONObject(new String(decryptedBytes, "UTF-8"));
      return jsonValues;
   }
  
  /*
   * Read in all bytes from stream and return in a byte array
   * @param is InputStream
   * @return byte array
   */
   private byte[] readAllBytesFromStream(InputStream is) {
      final int BUFFER_SIZE = 4096; // arbitrary buffer size
      // it is considered a bad idea to use is.available() to determine the amount
      // of data to read, so we just read everything into an expandable buffer
      ExpandableByteBuffer ebbuf = new ExpandableByteBuffer(BUFFER_SIZE);
      byte[] buffer = new byte[BUFFER_SIZE];
      int count = 0;
      try {
         count = is.read(buffer); // read in chunks
         while (count > -1) {
            ebbuf.put(buffer, count);
            count = is.read(buffer);
         }
      } catch (IOException e) {
         /* non-decryption stream problem */
         logger.log(Level.WARNING, "load: IOException " + e);
         buffer = null;
      }
      return ebbuf.getBytes();
   }
	
	/**
	 * Save secrets to encrypted JSON output stream. The stream must already be open and is
	 * closed on exit.
	 * 
	 * @param os
	 * @throws IOException
	 * @throws JSONException 
	 */
   public void saveSecretsToEncryptedJSONStream(OutputStream os) throws IOException, JSONException {
      CipherOutputStream cos = new CipherOutputStream(os, cipherInfo.encryptCipher);
      cos.write(toJSON().toString().getBytes("UTF-8"));
      try {
         cos.close();
      } catch (Exception ignore) {
      }
   }
  
  /**
   * Save secrets to JSON object.
   * 
   * The return is a JSON object which is effectively a bucket of key/value pairs.
   * Current processed key/values are:
   *  syncDate (String) - last sync date of secrets collection
   *  secrets (JSONArray) - secrets collection
   * 
   * @return JSONObject
   * @throws JSONException 
   */
   protected JSONObject toJSON() throws JSONException {
      JSONObject jsonValues = new JSONObject();
      jsonValues.put("syncdate", getLastSyncTimestamp());
      JSONArray jsonSecrets = new JSONArray();
      for (HostSecret secret : secrets) {
         jsonSecrets.put(secret.toJSON());
      }
      jsonValues.put("secrets", jsonSecrets);
      return jsonValues;
   }

	/**
	 * @return the cipherInfo
	 */
	public CipherInfo getCipherInfo() {
		return cipherInfo;
	}

	/**
	 * @param cipherInfo the cipherInfo to set
	 */
	public void setCipherInfo(CipherInfo cipherInfo) {
		this.cipherInfo = cipherInfo;
	}

   /**
    * (Re)create cipher
    * @param cipherInfo the cipherInfo to set
    */
   public void createCipherInfo() {
//      this.cipherInfo = SecurityUtils.createCiphers(pswdBytes, cipherInfo.salt, cipherInfo.rounds);
      this.cipherInfo = SecurityUtils.createCiphers(pswdBytes, null);
   }

	/**
   * @return the sourceName
   */
  public String getSourceName() {
    return sourceName;
  }

  /**
   * @param sourceName the sourceName to set
   */
  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  /**
	 * @return the pswdBytes
	 */
	public byte[] getPswdBytes() {
		return pswdBytes;
	}

	/**
	 * @param pswdBytes the pswdBytes to set
	 */
	public void setPswdBytes(byte[] pswdBytes) {
		this.pswdBytes = pswdBytes;
	}
}
