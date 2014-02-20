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

import java.awt.Window;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

/**
 * Class to encapsulate write access to phone secrets for feeding back errors.
 * 
 * A response code of 206 is used so that the phone can distinguish normal content from a status code. (An error code
 * eg. 500 could have been used but it makes the code at the phone end more complex.)
 * 
 * @author Chris Wood
 */
public class OutputErrorPhone extends DeviceConnection {

   /**
    * Sync operation was cancelled by user
    */
   public static final byte SYNC_CANCELLED = 1;

   /**
    * Decryption failure. The most likely cause is a wrong password
    */
   public static final byte DECRYPT_ERROR = 2;

   private static Logger logger = Logger.getLogger(OutputErrorPhone.class.getName());

   private WriteSecretsTask writeSecretsTask = new WriteSecretsTask();
   private byte status;

   /**
    * Constructor
    * 
    * @param parent
    */
   public OutputErrorPhone(Window parent) {
      super(parent);
      setSuppressWaitMessage(true); // don't need this message
   }

   /**
    * Set the task as cancelled
    */
   public void setCancelled() {
      getConnectionTask().cancel(true);
   }

   /*
    * Provide task ref to superclass
    * 
    * @see com.ceperman.pcsecrets.AbstractPhoneCommunicator#getConnectionTask()
    */
   @Override
   protected SwingWorker<DeviceSecretsCollection, Void> getConnectionTask() {
      return writeSecretsTask;
   }

   /**
    * Start the task and show connection dialog
    * 
    * Note: the thread blocks on showDialog(), so the execute() must be issued before this.
    * 
    * @param status
    *           phone status to send
    */
   public void start(byte status) {
      this.status = status;
      super.start();
   }

   /**
    * Task to send secrets to the phone
    */
   private class WriteSecretsTask extends SwingWorker<DeviceSecretsCollection, Void> {
      private Socket socket = null;

      /*
       * Constructor
       */
      public WriteSecretsTask() {
         super();
      }

      @Override
      /*
       * the signature has to specify a returned collection even though nothing is actually returned by this output
       * method
       */
      protected DeviceSecretsCollection doInBackground() throws Exception {
         setDialogText(Messages.getString("OutputErrorPhone.waitconnection"));

         socket = getConnectionSocket(); // get the socket stored during input

         OutputStream os = null;
         if (socket != null) {
            logger.log(Level.INFO, "Output socket is connected");
            try {
               os = socket.getOutputStream();
               os.write(status); // write the status byte
               os.close();

               logger.log(Level.FINE, "response data: " + status);
               logger.log(Level.FINE, "Error status sent to phone successfully");
            } catch (Exception e) {
               logger.log(Level.WARNING, "Communication exception sending error status to phone:  " + e);
            } finally {
               if (os != null) try {
                  os.close();
               } catch (IOException ignore) {
               }
            }
         } else {
            logger.log(Level.WARNING, "Phone is not receiving - terminating");
         }
         return null;
      }

      @Override
      protected void done() {
         phoneConnectionDialog.dispose(); // remove the dialog

         if (socket != null) try {
            socket.close();
         } catch (IOException ignore) {
         }
         setConnectionSocket(null);
      }

   }

}
