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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.ceperman.utils.Bytes;

/**
 * Class to encapsulate communication with phone. Holds the reference to the socket.
 * 
 * @author Chris Wood
 */
public abstract class DeviceConnection extends AbstractPhoneCommunicator {
	private static Socket connectionSocket = null;

	protected DeviceConnection(Window parent) {
		super(parent);
	}

	/**
	 * @return the connectionSocket
	 */
	protected static Socket getConnectionSocket() {
		return connectionSocket;
	}

	/**
	 * @param connectionSocket the connectionSocket to set
	 */
	protected static void setConnectionSocket(Socket httpPhone) {
		DeviceConnection.connectionSocket = httpPhone;
	}
   
	protected void writeData(byte[] data, OutputStream os) throws IOException {
      os.write(toByteArray(data.length), 0, 4);
      os.write(data, 0, data.length);
   }
   
	protected byte[] readData(InputStream is) throws IOException {
      byte[] dataLength = new byte[4];
      
      // read the length header
      is.read(dataLength, 0, 4);
      
      /* read the data - a single read is not sufficient because the 
       * data is not necessarily delivered in one chunk
       */
      
      byte[] data = new byte[fromByteArray(dataLength)];
      int offset = 0;
      int numRead = 0;
      while (offset < data.length
                  && (numRead = is.read(data, offset, data.length - offset)) >= 0) {
         offset += numRead;
      }
      
      return data;
   }
   
	protected byte[] toByteArray(int value) {
      return new byte[] {
          (byte) (value >> 24),
          (byte) (value >> 16),
          (byte) (value >> 8),
          (byte) value};
  }

	protected int fromByteArray(byte[] bytes) {
       return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
  }
  
	protected String bytesToHexTruncated(byte[] b) {
     int max = 32;
     String stringData = new String();
     if (b.length > max) {
        byte[] b_trunc = new byte[max];
        System.arraycopy(b, 0, b_trunc, 0, max);
        stringData = Bytes.byteArrayToHex(b_trunc) + "...";
     } else {
        stringData = Bytes.byteArrayToHex(b);
     }
     return stringData;
  }

}