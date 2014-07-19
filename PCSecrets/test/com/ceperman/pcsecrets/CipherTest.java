/**
 * 
 */
package com.ceperman.pcsecrets;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.json.JSONException;
import org.json.JSONObject;

import com.ceperman.pcsecrets.SecurityUtils.CipherInfo;
import com.ceperman.utils.Strings;


/**
 *
 * @author Chris Wood
 */
public class CipherTest {

	/**
	 * @param args
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws JSONException 
	 */
	public static void main(String[] args) throws IllegalBlockSizeException, BadPaddingException, JSONException {
		String pswd = null;
		String clearText = null;
		byte[] cipherText = null;
		CipherInfo cipherInfo = null;
		
		SecretsProperties.getInitialInstance(".pcsecrets-dev", false);
		SecurityUtils.checkBCProvider();
		
		pswd = "aaaa";
		cipherInfo = SecurityUtils.createCiphers(pswd.getBytes(), null);
		JSONObject jo = new JSONObject();
		jo.put("l", 999999999);
		clearText = jo.toString();
		cipherText = cipherInfo.encryptCipher.doFinal(clearText.getBytes());
		System.out.println("password: " + pswd + ", clearText: " + clearText + ", length: " + clearText.getBytes().length + 
				", cipherText: " + Strings.toHex(cipherText) + ", length: " + cipherText.length);
	}

}
