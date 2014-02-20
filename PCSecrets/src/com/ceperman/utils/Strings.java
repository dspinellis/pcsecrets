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

import java.math.BigInteger;

/**
 * Some string utility methods
 * 
 * @author Chris Wood
 */
public class Strings {

	/**
	 * Check if target array contains source string
	 * 
	 * @param source
	 * @param target
	 * @return true or false
	 */
	public static boolean isIn(String source, String[] target) {
		if (Strings.indexOf(source, target) < 0) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Get the index of the source in the target
	 * 
	 * @param source
	 * @param target
	 * @return index of string if found, -1 otherwise
	 */
	public static int indexOf(String source, String[] target) {
		for (int i = 0; i < target.length; i++) {
			if (target[i].trim().equals(source)) {
				return i;
			}
		}
		return -1;
	}

   /**
    * Get the index of the source in the target ignoring case
    * 
    * @param source
    * @param target
    * @return index of string if found, -1 otherwise
    */
   public static int indexOfIgnoreCase(String source, String[] target) {
      for (int i = 0; i < target.length; i++) {
         if (target[i].trim().equalsIgnoreCase(source)) {
            return i;
         }
      }
      return -1;
   }
	
	/**
	 * Returns a hex string representing the byte array
	 * 
	 * @param bytes
	 * @return hex string
	 */
	public static String toHex(byte[] bytes) {
	   StringBuilder sb = new StringBuilder();
      for(byte b: bytes)
         sb.append(String.format("%02x", b&0xff));
      return sb.toString();
	}
   
   /**
    * Convert ASCII bytes to hex string.
    * Uses String.format. The String representation of each byte
    * is the UTF-8 (default charset) value of that byte i.e. any
    * non-ASCII char (128-255) will not be shown correctly.
    * E.g. 0xa3 ("£") will be converted to 0xc2a3
    * 
    * @param bytes
    * @return hex string (of sorts!)
    */
   public static String asciiToHex(byte[] bytes) {
       BigInteger bi = new BigInteger(1, bytes);
       return String.format("%0" + (bytes.length << 1) + "x", bi);
   }
	
	/**
	 * Produce a hex string representation of a string
	 * 
	 * @param str
	 * @return string hex representation of input string
	 */
	public static String convertStringToHex(String str) {
	  char[] chars = str.toCharArray();
 
	  StringBuffer hex = new StringBuffer();
	  for(int i = 0; i < chars.length; i++) {
	    hex.append(Integer.toHexString((int)chars[i]).toUpperCase());
	  }
	  return hex.toString();
  }
	
	/**
	 * Produce a string from a hex representation of a string
	 * 
	 * @param hex - hex representation of input string
	 * @return converted string
	 */
	public static String convertHexToString(String hex) {		 
	  StringBuilder sb = new StringBuilder();
 
	  //49204c6f7665204a617661 split into two characters 49, 20, 4c...
	  for( int i=0; i<hex.length()-1; i+=2 ) {
	      //grab the hex in pairs
	      String output = hex.substring(i, (i + 2));
	      //convert hex to decimal
				try {
					int decimal = Integer.parseInt(output, 16);
		      //convert the decimal to character
		      sb.append((char)decimal);
				} catch (NumberFormatException e) {
					String msg = "At pos " + i + ", hex data is '" + convertStringToHex(output) + "'";
					NumberFormatException nfe = new NumberFormatException(msg);
					nfe.fillInStackTrace();
					throw nfe;
				}
	  }
	  return sb.toString();
  }
	
	/**
	 * Split key value pairs
	 * 
	 * @param values
	 * @return string array
	 */
	public static String[] splitKeyValuePairs(String values) {
		String noNewLine = values.replace("\n", " ");
		return noNewLine.split("=| +");
	}

}
