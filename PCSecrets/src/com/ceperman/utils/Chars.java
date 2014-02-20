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
 * Some character utility methods
 * @author Chris Wood
 */
public class Chars {
	
	/**
	 * Trim leading and trailing spaces
	 * @param chars
	 * @return char array
	 */
	public static char[] trim(char[] chars) {
		/* count the leading spaces */
		int leading = 0;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == ' ') {
				leading++;
			} else break;
		}
		/* count trailing spaces */
		int trailing = 0;
		for (int i = chars.length-1; i > -1; i--) {
			if (chars[i] == ' ') {
				trailing++;
			} else break;
		}
		
		if (chars.length - (leading + trailing) < 1) return new char[0];
		
		char[] retval = new char[chars.length - (leading + trailing)];
		System.arraycopy(chars, leading, retval, 0, retval.length);
		return retval;
	}
	
}
