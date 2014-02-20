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

import java.awt.Frame;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.mindrot.jbcrypt.BCrypt;

import com.ceperman.pcsecrets.SecurityUtils.CipherInfo;
import com.ceperman.pcsecrets.SecurityUtils.CipherParms;

/**
 * Class to encapsulate read access to the phone. Communication uses simple socket protocol as
 * communication will be between local nodes. The socket is not closed when input is finished
 * to allow for a response. A reference to the client (phone) socket is stored in the parent class.
 * 
 * @author Chris Wood
 */
public class InputPhone extends DeviceConnection {
   private static Logger logger = Logger.getLogger(InputPhone.class.getName());

   private GetSecretsTask getSecretsTask = new GetSecretsTask();
   private int port;
   private byte[] password;
   private String metadataToSend = "none";
   private String metadataReceived;

   /*
    * Status distinguishes between these possibilities: (1) comms error prevented receiving anything (2) comms OK but no
    * data received (3) data received but was not decrypted successfully (4) secrets data was decrypted successfully
    * 
    * In all cases except (1) a response should eventually be sent.
    */
   private boolean commsError; // comms error occurred
   private boolean decryptionError; // data not decrypted successfully
   private boolean successful; // secrets data received OK

   /**
    * Constructor
    * 
    * @param parent
    * @param listener
    * @param port
    * @param password
    */
   public InputPhone(Frame parent, PropertyChangeListener listener, int port, byte[] password) {
      super(parent);
      this.port = port;
      this.password = password;
      getSecretsTask.addPropertyChangeListener(listener);
   }

   /*
    * Provide task ref to superclass
    * 
    * @see com.ceperman.pcsecrets.AbstractPhoneCommunicator#getConnectionTask()
    */
   @Override
   protected SwingWorker<DeviceSecretsCollection, Void> getConnectionTask() {
      return getSecretsTask;
   }

   /**
    * Get result from background task
    * 
    * @return EncryptableSecretsCollection or null if error
    */
   public DeviceSecretsCollection getPhoneSecrets() {
      try {
         return getSecretsTask.get();
      } catch (Exception e) {
         logger.log(Level.WARNING, "Exception getting phone secrets: " + e.getMessage());
      }
      return null;
   }

   /**
    * @return true if a comms error occurred, false otherwise
    */
   public boolean isCommsError() {
      return commsError;
   }

   /**
    * @return true if a decryption error occurred, false otherwise
    */
   public boolean isDecryptionError() {
      return decryptionError;
   }
   
   /**
    * Set metadata to send to device
    * @param metadata
    */
   public void setMetadata(String metadata) {
      metadataToSend = metadata;
   }
   
   /**
    * Get metadata received from device
    * @return metadata
    */
   public String getMetadata() {
      return metadataReceived;
   }

   /**
    * Task to retrieve secrets from the phone
    */
   private class GetSecretsTask extends SwingWorker<DeviceSecretsCollection, Void> {
      private ServerSocket serverSocket = null;

      /*
       * Constructor
       */
      public GetSecretsTask() {
         super();
      }

      @Override
      protected DeviceSecretsCollection doInBackground() throws Exception {
         DeviceSecretsCollection phoneSecrets = null;
         setDialogText(Messages.getString("PhoneCommunicator.waitconnection"));
         
         Socket client = null;
         InputStream is = null;
         OutputStream os = null;
         try {
            serverSocket = new ServerSocket(port);
            logger.log(Level.FINE, "Server socket " + port + " waiting");
            client = serverSocket.accept();
            logger.log(Level.INFO, "Device connected");

            /* set the socket reference for the data response */
            setConnectionSocket(client);
            
            // send metadata
            os = client.getOutputStream();
            writeData(metadataToSend.getBytes(CHARSET), os);
            logger.log(Level.FINE, "metadata sent: " + new String(metadataToSend) + " (" + metadataToSend.length() + ")");
            
            // receive data
            is = client.getInputStream();
            byte[] requestData = readData(is);
            logger.log(Level.FINE, "data received: " + bytesToHexTruncated(requestData) + " (" + requestData.length + ")");
            
            phoneSecrets = extractSecretsCollection(requestData);
            if (phoneSecrets != null) {
               phoneSecrets.setSourceName(client.getInetAddress().getHostAddress());
            }
         } catch (Exception e) {
            logger.log(Level.WARNING, "Exception getting device secrets: " + e);
            if (is != null) try {
               is.close();
            } catch (IOException ignore) {
            }
            setConnectionSocket(null);
            commsError = true;
         }

         if (phoneSecrets != null) {
            logger.log(Level.INFO, "Received device secrets: " + phoneSecrets.getSize());
         } else {
            logger.log(Level.WARNING, "Nothing received from phone");
         }
         return phoneSecrets;
      }

      @Override
      protected void done() {
         if (serverSocket != null) try {
            serverSocket.close();
         } catch (IOException ignore) {
         }
         phoneConnectionDialog.dispose(); // remove the dialog

         if (isCancelled()) {
            logger.log(Level.INFO, "done() signalled and cancelled");
            firePropertyChange(Constants.PHONE_RECEIVE, null, Constants.CANCEL);
         } else if (successful) {
            logger.log(Level.INFO, "done() signalled and successful");
            firePropertyChange(Constants.PHONE_RECEIVE, null, Constants.COMPLETE);
         } else {
            logger.log(Level.INFO, "done() signalled and not successful");
            firePropertyChange(Constants.PHONE_RECEIVE, null, Constants.FAILED);
         }
      }

      /**
       * Extract the secrets collection.
       * 
       * @param phoneSecrets
       * @param client
       * @param requestData
       * @return
       * @throws IOException
       */
      private DeviceSecretsCollection extractSecretsCollection(byte[] requestData) throws IOException {
         DeviceSecretsCollection phoneSecrets = null;
         ByteArrayInputStream bais = new ByteArrayInputStream(requestData);
         CipherInfo cipherInfo = null;
         CipherParms parms = SecurityUtils.getCipherParms(bais);
         if (parms.rounds > 0 && parms.salt.length == BCrypt.BCRYPT_SALT_LEN) {
            /* the contents of the file look valid */
            cipherInfo = SecurityUtils.createCiphers(password, parms); // create cipher
            int remaining = bais.available();
            logger.log(Level.FINE, "Cipher created, encrypted data remaining: " + remaining);
            try {
               DeviceSecretsCollection tempSecrets = new DeviceSecretsCollection();
               tempSecrets.setPswdBytes(password);
               tempSecrets.setCipherInfo(cipherInfo);
               tempSecrets.loadSecretsFromEncryptedJSONStream(bais);
               phoneSecrets = tempSecrets; // no error occurred
               successful = true;
            } catch (Exception e) {
               decryptionError = true;
               logger.log(Level.WARNING, "Exception decrypting phone secrets: " + e.getMessage());
            }
         } else {
            /* contents do not look valid */
            logger.log(Level.WARNING, "Encryption problem with incoming data - rounds: " + parms.rounds
                        + ", salt length: " + (parms.salt == null ? "null" : parms.salt.length));
            decryptionError = true;
         }

         return phoneSecrets;         
      }

   }

}
