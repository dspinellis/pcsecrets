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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.ceperman.pcsecrets.SecurityUtils.CipherInfo;
import com.ceperman.pcsecrets.SecurityUtils.CipherParms;
import com.ceperman.utils.Bytes;
import com.ceperman.utils.ExpandableByteBuffer;
import com.ceperman.utils.Strings;

/**
 * Represents a persistent collection of secrets.
 * 
 * The stored format of the collection looks like this:
 * 
 * <-------------------------------  file data  ------------------------------>
 * <-security hdr-><------encrypted data---------><----undecryptable data----->
 * 
 * (1) an unencrypted security header containing cipher parameters
 * (2) encrypted data, consisting of two sections
 * (3) undecryptable data (not encrypted with the current password)
 * 
 * The security header contains the salt and rounds values needed to decrypt the
 * encrypted data (when combined with the user-supplied password).
 * 
 * <------------------------ encrypted data -------------------------->
 * <-length hdr-><-secrets section-><-length hdr-><-deletions section->
 * 
 * The encrypted data consists of two sections:
 * (1) secrets data
 * (2) deletions data (sync devices)
 * Both sections have a separately encrypted header that specifies the length
 * of the section. This allows the sections to be isolated and their data
 * decrypted separately.
 * 
 * <-------------------------undecryptable data---------------------->
 * <---random bytes---><------encrypted data---------><-security hdr->
 *                      <<< encryption direction
 * 
 * The undecryptable section potentially contains a second set of secrets,
 * encrypted under a second, optional, password. However, it always contains some
 * random data so that the existence of a second set of secrets is not detectable
 * from the file size (plausible deniability).
 * 
 * The encrypted data in the undecryptable section is stored in reverse bit order.
 * If decryption of the current encrypted data fails, the whole of the encrypted
 * section is reversed bitwise and the decryption retried.
 * 
 * @author chris
 */
@SuppressWarnings("serial")
public class StoredSecretsCollection extends EncryptableSecretsCollection {
	private static Logger logger = Logger.getLogger(StoredSecretsCollection.class.getName());
	
	/* length of encrypted length field - this is the minimum AES cipher block size
	 * and can hold a length value up to 999999999 (~1GB) */
	private static final int ENCRYPTED_LENGTH_FIELD = 16;
	
	/* deletions data */
	private SyncDeviceCollection syncDevices = new SyncDeviceCollection();
	
	/* The undecrypted part of a stored secrets file */
	private byte[] undecryptedBytes; 
	
	/** 
	 * Default constructor
	 */
	public StoredSecretsCollection() {
		super();
	}

  /**
   * Save the secrets to the file identified by this collection.
   */
  public void save() {
     saveAs(getSourceName());
  }
  
  /**
   * Save the secrets to the specified file.
   * @param fileName target filename
   */
  public void saveAs(String fileName) {
    File secretsFile = new File(fileName);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(secretsFile);
      SecurityUtils.writeSecurityHeader(getCipherInfo().parms, fos);
      writeEncryptedData(fos);
      
      /* lastly the undecryptable data */
      if (undecryptedBytes != null && undecryptedBytes.length > 0) {
        fos.write(undecryptedBytes);
        if (logger.isLoggable(Level.FINE)) {
           logger.log(Level.FINE, "save: undecryptable data length " + undecryptedBytes.length);
           logger.log(Level.FINE, "save: overall file size " + secretsFile.length());
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "save: " + e.getMessage());
    } finally {
      try {
        if (fos != null) {
          fos.close();
        }
      } catch (IOException e) {} /* ignore any problem here */
    }
  }
  
  /**
   * Write the encrypted data (see class description) to the specified output
   * stream, including the security header.
   * 
   * @param os
   */
  public void writeEncryptedData(OutputStream os) {
     try {
        /* create the encrypted data section */
        // cannot write encrypted data directly to FileOutputStream because the 
        // CipherStream must be closed to write out all data (flush() doesn't
        // do this) and this would close the underlying stream.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        saveSecretsToEncryptedJSONStream(baos);
        byte[] encryptedSecrets = baos.toByteArray();
        // write the secrets section 
        writeHeaderedSection(os, encryptedSecrets);
        logger.log(Level.FINE, "writeEncryptedData: " + getSize() + " secrets written");
        // write the deleted secrets section
        byte[] encryptedDeletions = getCipherInfo().encryptCipher.doFinal(syncDevices.toJSON().getBytes("UTF-8"));
        writeHeaderedSection(os, encryptedDeletions);
        logger.log(Level.FINE, "writeEncryptedData: " + syncDevices.getSize() + " sync devices written");
     } catch (Exception e) {
        logger.log(Level.SEVERE, "writeEncryptedData: " + e.getMessage());
     }
  }
  
  /*
   * Write the data to the output stream with a length header. The length
   * header is an encrypted JSON object
   */
  private void writeHeaderedSection(OutputStream os, byte[] data) throws Exception {
     JSONObject jo = new JSONObject();
     jo.put("l", data.length);
     byte[] encryptedHeader = getCipherInfo().encryptCipher.doFinal(jo.toString().getBytes("UTF-8"));
     os.write(encryptedHeader); // encrypted length value, fixed 16 byte block
     os.write(data);
     if (logger.isLoggable(Level.FINE)) {
        logger.log(Level.FINE, "writeHeaderedSection: data length " + data.length
                    + ", hdr length: " + encryptedHeader.length +
                    ", hdr (encrypted) " + Strings.toHex(encryptedHeader));
     }
  }
  
  /**
   * Load the secrets from the file contents.
   * 
   * Try to load using the "normal" encrypted data. If this fails, reverse
   * the contents bitwise and retry.
   * 
   * @param fileBytes byte content of the secrets file
   * @return true if data loaded, false otherwise
   */
   public boolean load() {
      boolean loaded = false;

      byte[] fileBytes = loadFile(new File(getSourceName()));
      if (fileBytes == null) {
         logger.log(Level.FINE, "load: file not loaded");
         return false;
      }

      logger.log(Level.FINE, "load: loading from set 1");
      loaded = loadEncrypted(fileBytes);
      if (!loaded) {
         /* if not loaded, reverse bytes and try again */
         byte[] reversedBytes = Bytes.reverseBits(fileBytes);
         logger.log(Level.FINE, "load: loading from set 2");
         loaded = loadEncrypted(reversedBytes);
         if (loaded) {
            logger.log(Level.FINE, "load: secrets loaded from set 2");
         }
      } else {
         logger.log(Level.FINE, "load: secrets loaded from set 1");
      }
      
      if (loaded) {
         logger.log(Level.FINE, "load: " + getSize() + " secrets loaded");
      } else {
         logger.log(Level.FINE, "load: secrets not loaded");
      }
      
      return loaded;
   }
	  
	  /* Load the file into memory.
	   * 
	   * @param secretsFile
	   * @return
	   */
	  private byte[] loadFile(File secretsFile) {
	     logger.log(Level.FINE, "loadFile: overall file size " + secretsFile.length());
	     final int BUFFER_SIZE = 4096; // arbitrary buffer size
	     byte[] fileBytes = null;
	     FileInputStream fis = null;
	     try {
	        fis = new FileInputStream(secretsFile);
	        ExpandableByteBuffer ebbuf = new ExpandableByteBuffer(BUFFER_SIZE);
	        byte[] buffer = new byte[BUFFER_SIZE];
	        int count = 0;
	        count = fis.read(buffer); // read in chunks
	        while (count > -1) {
	           ebbuf.put(buffer, count);
	           count = fis.read(buffer);
	        }
	        fis.close();
	        fileBytes = ebbuf.getBytes();
	     } catch (IOException e) {
	        /* non-decryption stream problem */
	        logger.log(Level.WARNING, "loadFile: IOException " + e);
	     } finally {
	        try {
	           if (fis != null) fis.close();
	        } catch (IOException e) {} //ignore
	     }
	     return fileBytes;
	  }
	
	/*
	 * Load from the encrypted data.
	 * 
	 * The data stream contains:
	 *   security header containing the cipher parms
	 *   a length-headed section containing the secrets
	 *   an optional length-headed section containing deletion info
	 *   undecypherable data (junk plus optional reverse-encrypted second secrets
	 *   set).
	 * 
	 * For backward compatibility, the deletions section may be missing.
	 */
	private boolean loadEncrypted(byte[] bytes) {
	   int offset = 0;
	   String section = "security header";
	   
	   try {
	      // get cipher parms and create encrypt/decrypt ciphers
	      CipherParms parms = null;
	      {
	         ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
	         parms = SecurityUtils.getCipherParms(bais);
	         bais.close();
	      }
	      CipherInfo cipherInfo = SecurityUtils.createCiphers(getPswdBytes(), parms);
	      setCipherInfo(cipherInfo);         
	      
	      offset = SecurityUtils.SECURITY_HDR_LENGTH;
	      
	      // get secrets
	      section = "secrets";
	      byte[] secretsData = getHeaderedSectionData(bytes, offset);
	      loadSecretsFromEncryptedJSONStream(new ByteArrayInputStream(secretsData));
         offset += ENCRYPTED_LENGTH_FIELD + secretsData.length;

	      // get sync devices
	      try {
            section = "deletions";
            byte[] deletionsData = getHeaderedSectionData(bytes, offset);
            String jsonString = new String(getCipherInfo().decryptCipher.doFinal(deletionsData), "UTF-8");
            syncDevices.fromJSON(jsonString);
            logger.log(Level.FINE, "loadEncrypted: retrieved sync devices: " + syncDevices.size());
            offset += ENCRYPTED_LENGTH_FIELD + deletionsData.length;
         } catch (Exception e) {
            // assume error here means the section is missing - not an error
            logger.log(Level.FINE, "loadEncrypted: decryption failed for " + section + " section (" + e + ")");
         }

	      // get the undecypherable data
	      undecryptedBytes = Arrays.copyOfRange(bytes, offset, bytes.length);
	      logger.log(Level.FINE, "loadEncrypted: undecryptable data length " + undecryptedBytes.length);
	      return true;
	   } catch (Exception e) {
	      // almost certainly caused by bad decryption (wrong password, wrong keylength, etc)
	      logger.log(Level.FINE, "loadEncrypted: decryption failed for " + section + " section (" + e + ")");
	   }
	   return false;
	}
   
   /*
    * Get the data of a headered section.
    * 
    * The offset locates the start of the section. The section (should) start
    * with an encrypted length field. This routine decrypts the header, throwing
    * an exception if the decryption fails.
    * 
    * It then extracts and returns the section data undecrytped. 
    */
   private byte[] getHeaderedSectionData(byte[] bytes, int offset) throws Exception {
      // extract the length header
      byte[] encryptedHeader = Arrays.copyOfRange(bytes, offset, offset + ENCRYPTED_LENGTH_FIELD);
      // decrypt the length value
      byte[] jsonBytes = getCipherInfo().decryptCipher.doFinal(encryptedHeader);
      String jsonString = new String(jsonBytes);
      JSONObject jo = (JSONObject) new JSONTokener(jsonString).nextValue();
      int dataLength = jo.getInt("l");
      // return the section data
      if (logger.isLoggable(Level.FINE)) {
         logger.log(Level.FINE, "getHeaderedSection: data length " + dataLength
                     + ", hdr length: " + encryptedHeader.length +
                     ", hdr (encrypted) " + Strings.toHex(encryptedHeader));
      }
      return Arrays.copyOfRange(bytes, offset + ENCRYPTED_LENGTH_FIELD, offset + ENCRYPTED_LENGTH_FIELD
                  + dataLength);
   }

	/**
	 * @param undecryptedBytes the undecryptedBytes to set
	 */
	public void setUndecryptedBytes(byte[] undecryptedBytes) {
		this.undecryptedBytes = undecryptedBytes;
	}

   /**
    * @return the syncDevices
    */
   public SyncDeviceCollection getSyncDevices() {
      return syncDevices;
   }

   /* 
    * Include the status of the sync device collection in the test
    */
   @Override
   public boolean isChanged() {
      return super.isChanged() | syncDevices.isChanged();
   }

   /* (non-Javadoc)
    * Also set the sync device collection indicator
    * @see com.ceperman.pcsecrets.SecretsCollection#setChanged(boolean)
    */
   @Override
   public void setChanged(boolean changed) {
      super.setChanged(changed);
      syncDevices.setChanged(changed);
   }
}
