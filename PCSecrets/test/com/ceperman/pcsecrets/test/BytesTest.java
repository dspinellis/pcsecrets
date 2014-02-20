package com.ceperman.pcsecrets.test;

import java.util.Arrays;

import junit.framework.TestCase;

import com.ceperman.utils.Bytes;

/**
 *
 * @author Chris Wood
 */
public class BytesTest extends TestCase {
	private static final byte[] testBytes1 = new byte[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', ' ', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
	private static final byte[] testBytes2 = new byte[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd', ' ', ' ', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
	private static final byte[] testBytes3 = new byte[] { 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };
	
	private static final byte[] testBytes10 = new byte[] { (byte) 0x03, (byte) 0x44 };
	private static final byte[] testBytes11 = new byte[] { (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef };
	private static final byte[] testBytes12 = new byte[] { (byte) 0xf7, (byte) 0xb3, (byte) 0xd5, (byte) 0x91, (byte) 0xe6, (byte) 0xa2, (byte) 0xc4, (byte) 0x80 };

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	/**
	 * Test splitOnce()
	 */
	public void testSplitOnce() {
		byte[][] result = Bytes.splitOnce(testBytes1);
		assertTrue(result.length == 2);
		assertTrue(result[0].length == 8);
		assertTrue(result[1].length == 8);
		
		result = Bytes.splitOnce(testBytes2);
		assertTrue(result.length == 2);
		assertTrue(result[0].length == 8);
		assertTrue(result[1].length == 9);
		
		result = Bytes.splitOnce(testBytes3);
		assertTrue(result.length == 1);
		assertTrue(result[0].length == 8);
	}
	
	/**
	 * Test trim()
	 */
	public void testTrim() {
		byte[][] result = Bytes.splitOnce(testBytes2);
		assertTrue(Bytes.trim(result[1]).length == 8);
	}
	
	/**
	 * Test testReverseBits()
	 */
	public void testReverseBits() {
		byte[] result = Bytes.reverseBits(testBytes10);
		assertTrue(result.length == 2);
		assertTrue(result[0] == (byte) 0x22);
		assertTrue(result[1] == (byte) 0xc0);
		
		result = Bytes.reverseBits(testBytes11);
		assertTrue(result.length == 8);
		assertTrue(Arrays.equals(result, testBytes12));

		result = Bytes.reverseBits(testBytes12);
		assertTrue(result.length == 8);
		assertTrue(Arrays.equals(result, testBytes11));
	}
	
	/**
	 * Test longValue()
	 */
	public void testLong() {
		byte[] input = new byte[]{ (byte)0x01 };
		assertTrue(Bytes.getLong(input) == 1L);
		input = new byte[]{ (byte)0xff };
		assertTrue(Bytes.getLong(input) == 255L);
		input = new byte[]{ (byte)0x01, (byte)0x00 };
		assertTrue(Bytes.getLong(input) == 256L);
		input = new byte[]{ (byte)0x01, (byte)0x23, (byte)0x45 };
		assertTrue(Bytes.getLong(input) == 74565L);
		input = new byte[]{ (byte)0x01, (byte)0x23, (byte)0x45, (byte)0x67 };
		assertTrue(Bytes.getLong(input) == 19088743L);
		assertTrue(Bytes.getLong(testBytes10) == 836L);
		assertTrue(Bytes.getLong(testBytes11) == 81985529216486895L);
	}

}
