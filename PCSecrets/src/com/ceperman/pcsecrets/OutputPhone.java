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
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

/**
 * Class to encapsulate write access to phone secrets
 * 
 * @author Chris Wood
 */
public class OutputPhone extends DeviceConnection {
	private static Logger logger = Logger.getLogger(OutputPhone.class.getName());
	
	private static final byte STATUS_OK = 0;
	
	private WriteSecretsTask writeSecretsTask = new WriteSecretsTask();
	/* package access */ DeviceSecretsCollection phoneSecrets;
	
	private boolean successful;
	
	/**
	 * Constructor
	 * 
	 * @param parent
	 * @param listener
	 * @param port 
	 */
	public OutputPhone(Window parent, PropertyChangeListener listener) {
		super(parent);
		writeSecretsTask.addPropertyChangeListener(listener);
		setSuppressWaitMessage(true); // don't need this message
	}
	
	/**
	 * Set the task as cancelled
	 */
	public void setCancelled() {
		getConnectionTask().cancel(true);
	}

	/* Provide task ref to superclass
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
	 * Note: the thread blocks on showDialog(), so the execute() must be issued
	 * before this.
	 * @param phoneSecrets phone secrets to send
	 */
	public void start(DeviceSecretsCollection phoneSecrets) {
		this.phoneSecrets = phoneSecrets;
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
		/* the signature has to specify a returned collection even though
		 * nothing is actually returned by this output method */
		protected DeviceSecretsCollection doInBackground() throws Exception {
			setDialogText(Messages.getString("OutputPhone.waitconnection"));
			
			socket = getConnectionSocket(); // get the socket stored during input
			
			OutputStream os = null;
			InputStream is = null;
			if (socket != null) {
				logger.log(Level.INFO, "Output socket is connected");
            try {
               // create the output
               os = socket.getOutputStream();
               os.write(STATUS_OK); // write the status byte
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               phoneSecrets.saveSecretsToEncryptedJSONStream(baos);
               byte[] responseData = baos.toByteArray();
               writeData(responseData, os);
               logger.log(Level.FINE, "data sent: " + bytesToHexTruncated(responseData) + " (" + responseData.length
                           + ")");
               logger.log(Level.INFO, "Client records written: " + phoneSecrets.getSize());
               
               // receive confirmation
               is = socket.getInputStream();
               if (is.read() == 0xff) {
                  logger.log(Level.FINE, "Response confirmation received");
                  successful = true;
                  logger.log(Level.INFO, "Sync operation success");
               }
            } catch (Exception e) {
               logger.log(Level.WARNING, "Communication exception:  " + e);
            } finally {
               if (os != null) try {
                  os.close();
               } catch (IOException ignore) {
               }
            }
			} else {
				logger.log(Level.WARNING, "Device is not receiving - terminating");
			}
			return null;
		}
		
		@Override
		protected void done() {
			phoneConnectionDialog.dispose(); // remove the dialog
			
			if (socket != null) try { socket.close(); } catch (IOException ignore) {}
			setConnectionSocket(null);

			if (isCancelled()) {
				logger.log(Level.INFO, "done() signalled and cancelled");
				firePropertyChange(Constants.PHONE_SEND, null, Constants.CANCEL);
			} else if (successful) {
				logger.log(Level.INFO, "done() signalled and successful");
				firePropertyChange(Constants.PHONE_SEND, null, Constants.COMPLETE);
			} else {
				logger.log(Level.INFO, "done() signalled and not successful");
				firePropertyChange(Constants.PHONE_SEND, null, Constants.FAILED);
			}
		}
		
	}

}
