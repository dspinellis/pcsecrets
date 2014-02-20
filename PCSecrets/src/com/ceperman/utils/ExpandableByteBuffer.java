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

package com.ceperman.utils;

/**
 * Expandable byte array
 * @author Chris Wood
 */
public class ExpandableByteBuffer {
	private byte[] bytes;
	private int increment;
	private int next; // index to next byte position (= byte count)
	
	/**
	 * Constructor
	 * 
	 * @param increment initial size and expansion increment
	 */
	public ExpandableByteBuffer(int increment) {
		this.increment = increment;
		bytes = new byte[increment];
	}
	
	/**
	 * Add byte to byte array. Allocate a larger array
	 * if full.
	 * 
	 * @param b
	 */
	public void put(byte b) {
		if (next == bytes.length) {
			byte[] newBytes = new byte[bytes.length + increment];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			bytes = newBytes;
		}
		bytes[next++] = b;	
	}
	
	/**
	 * Add byte array to byte array.
	 * 
	 * @param b
	 * @param count 
	 */
	public void put(byte[] b, int count) {
		for (int i = 0; i < count; i++) {
			put(b[i]);
		}
	}
	
	/**
	 * Return number of bytes held
	 * 
	 * @return byte count
	 */
	public int size() {
		return next;
	}
	
	/**
	 * Return bytes
	 * @return byte array
	 */
	public byte[] getBytes() {
		byte[] newBytes = new byte[next];
		System.arraycopy(bytes, 0, newBytes, 0, next);
		return newBytes;
	}
}
