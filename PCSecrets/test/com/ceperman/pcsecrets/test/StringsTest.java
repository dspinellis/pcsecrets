package com.ceperman.pcsecrets.test;

import java.io.UnsupportedEncodingException;

import org.bouncycastle.util.Arrays;

import junit.framework.TestCase;

import com.ceperman.utils.Strings;

/**
 * Test Strings
 * @author Chris Wood
 */
public class StringsTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	/**
	 * @throws UnsupportedEncodingException 
	 * 
	 */
	public void testOne() throws UnsupportedEncodingException {
		String testString = "The quick brown fox";
		System.out.println("bytes (toHex):               " + Strings.toHex(testString.getBytes()));
		System.out.println("string (convertStringToHex): " + Strings.convertStringToHex(testString));
		System.out.println("convert from tohex:              " + Strings.convertHexToString(Strings.toHex(testString.getBytes())));
		System.out.println("convert from convertStringToHex: " + Strings.convertHexToString(Strings.convertStringToHex(testString)));
		
		testString = "!\"£$%^&*()_+1234567890-={}[]:@~#';<>?/.,";
		System.out.println("bytes (toHex):               " + Strings.toHex(testString.getBytes()));
		System.out.println("string (convertStringToHex): " + Strings.convertStringToHex(testString));
		System.out.println("convert from tohex:              " + Strings.convertHexToString(Strings.toHex(testString.getBytes())));
		System.out.println("convert from convertStringToHex: " + Strings.convertHexToString(Strings.convertStringToHex(testString)));
		
		byte[] testBytes = { (byte)0x7f, (byte)0x80, (byte)0xa3, (byte)0xf3 };
		System.out.println("bytes - bytes (toHex):               " + Strings.toHex(testBytes));
		System.out.println("bytes - string (convertStringToHex): " + Strings.convertStringToHex(new String(testBytes, "ASCII")));
		System.out.println("bytes - convert from tohex:              " + Strings.convertHexToString(Strings.toHex(testBytes)));
		System.out.println("bytes - convert from tohex:              " + Arrays.areEqual(Strings.convertHexToString(Strings.toHex(testBytes)).getBytes(), testBytes));
		System.out.println("bytes - convert from convertStringToHex: " + Strings.convertHexToString(Strings.convertStringToHex(new String(testBytes, "ASCII"))));
	}
	
	/**
	 * 
	 */
	public void testTwo() {
		String testString = "address=192.168.0.3\n port=9100\n readTimeout=30000";
		String[] splits = Strings.splitKeyValuePairs(testString);
		for (String string : splits) {
			string = string.trim();
			System.out.println(string + " " + string.length());
		}
	}

}
