package com.ceperman.pcsecrets.test;

import com.ceperman.utils.Chars;

import junit.framework.TestCase;

/**
 * Test Chars
 * @author Chris Wood
 */
public class CharsTest extends TestCase {
	private static final char[] testChars1 = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
	private static final char[] testChars2 = new char[] { ' ', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
	private static final char[] testChars3 = new char[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', ' ' };
	private static final char[] testChars4 = new char[] { ' ', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', ' ' };
	private static final char[] testChars5 = new char[] { ' ', ' ', ' ', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', ' ', ' ' };
	private static final char[] testChars6 = new char[] { ' ', 'p', 'a', 's', 's', ' ', ' ', 'w', 'o', 'r', 'd', ' ' };
	private static final char[] testChars7 = new char[] { ' ', ' ', ' ', 'p', 'a', 's', 's', ' ', ' ', 'w', 'o', 'r', 'd', ' ', ' ', ' ', ' ' };
	private static final char[] testChars8 = new char[] { ' ' };
	private static final char[] testChars9 = new char[] { ' ', ' ' };

	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * Test trim()
	 */
	public void testTrim() {
		assertTrue(Chars.trim(testChars1)[1] != ' ' && Chars.trim(testChars1).length == 8);
		assertTrue(Chars.trim(testChars2)[1] != ' ' && Chars.trim(testChars2).length == 8);
		assertTrue(Chars.trim(testChars3)[1] != ' ' && Chars.trim(testChars3).length == 8);
		assertTrue(Chars.trim(testChars4)[1] != ' ' && Chars.trim(testChars4).length == 8);
		assertTrue(Chars.trim(testChars5)[1] != ' ' && Chars.trim(testChars5).length == 8);
		assertTrue(Chars.trim(testChars6)[1] != ' ' && Chars.trim(testChars6).length == 10);
		assertTrue(Chars.trim(testChars7)[1] != ' ' && Chars.trim(testChars7).length == 10);
		assertTrue(Chars.trim(testChars8).length == 0);
      assertTrue(Chars.trim(testChars9).length == 0);
	}

}
