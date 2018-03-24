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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.mindrot.jbcrypt.BCrypt;

import com.ceperman.utils.Bytes;
import com.ceperman.utils.Strings;

/**
 * Encryption overview
 * 
 * The same encryption mechanism is used in PCSecrets and Secrets for Android.
 * It uses bcrypt, which is a key derivation function for passwords designed by
 * Niels Provos and David Mazières and based on the Blowfish cipher. It implements
 * "key stretching" whereby a random salt is incorporated into the user-provided
 * password to make the encryption key more complex in order to be more resistant
 * to dictionary attacks. It is also notable for its expensive key setup phase
 * which is an adaptive algorithm: over time, the iteration count can be increased
 * to make it slower, so it remains resistant to brute-force search attacks even
 * with increasing computation power.
 * 
 * (The terms "bcrypt" and "key stretching" are described in detail in Wikipedia.)
 * 
 * The Blowfish cipher is implemented by the bcrypt() function. It takes as input
 * the user password, a random string (the "salt") and an iteration number
 * ("rounds", determined from the speed of the current processor). From this a
 * hashed password is generated. The password hash is used by the encryption
 * cipher as the key.
 * 
 * The process is symmetrical, so the same salt and iteration count must be
 * provided for decryption.
 * 
 * The implementation uses blowfish encryption. Encryption/decryption is implemented
 * by the blowfish provider jar (bouncycastle.com). The key hashing mechanism is
 * provided by bcrypt (mindrot.org).
 * 
 * @author Chris Wood
 */
public class SecurityUtils {
	private static Logger logger = Logger.getLogger(SecurityUtils.class.getName());
	
        // Factory to use for version 2 of encryption.
        private static final String KEY_FACTORY_V2 = "AES";
        private static final String CIPHER_FACTORY_V2 = "AES/CBC/PKCS5Padding";

        private static final String KEY_FACTORY = "AES";
        private static final String CIPHER_FACTORY = "AES/CBC/PKCS5Padding";

	/* encrypted file header id */
	static final byte[] SIGNATURE = {0x22, 0x34, 0x56, 0x79};
	
	static final int SECURITY_HDR_LENGTH = 25;
	
	/** Return value of createCiphers function. */
	static class CipherInfo { /* package access */
		Cipher encryptCipher;
		Cipher decryptCipher;
		CipherParms parms;
	}
	
	/** Cipher creation values. */
	static class CipherParms { /* package access */
	   int keylen;
      byte[] salt;
      int rounds;
      
      CipherParms(int keylen, byte[] salt, int rounds) {
         this.keylen = keylen;
         this.salt = salt;
         this.rounds = rounds;
      }
      
      CipherParms(CipherParms parms) {
         if (parms != null) {
            this.keylen = parms.keylen;
            this.salt = parms.salt;
            this.rounds = parms.rounds;            
         }
      }
	}

	/**
	 * Creates a new unique random salt.
	 * 
	 * @return A new salt value used to generate the secret key.
	 */
	private static byte[] createNewSalt() {
		byte[] bytes = new byte[BCrypt.BCRYPT_SALT_LEN];
		SecureRandom random = new SecureRandom();
		random.nextBytes(bytes);
		return bytes;
	}
	
	/**
	 * Add the BouncyCastle JCE provider if not already available
	 */
	public static void checkBCProvider() {
        logger.log(Level.FINE, "Checking BouncyCastle JCE provider");
		if (Security.getProvider("BC") == null) {
			Security.setProperty("crypto.policy", "unlimited");
            logger.log(Level.FINE, "Adding BouncyCastle JCE provider");
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			logger.log(Level.FINE, "Added BouncyCastle JCE provider successfully");
		}
	}

	/**
	 * Determines the ideal number of rounds to use for the bcrypt algorithm.
	 * More rounds are more secure, but require more time to log into Secrets.
	 * This function tries to balance security and convenience.
	 * 
	 * Each round increment doubles the amount of work required by bcrypt to
	 * generate a key. This function assumes that time is proportional to work.
	 * So for example, if 4 rounds takes 0.1 seconds to generate a key, 5 rounds
	 * will take 0.2 seconds, 6 rounds 0.4 seconds, and so on. The assumption
	 * will be that the key must be generated in less than 0.9 seconds to remain
	 * convenient for the user.
	 * 
	 * This function calculate how long it takes to generate a key using 4
	 * rounds on the current device, then determines the maximum number of
	 * rounds such that the time to generate will remain below the convenience
	 * threshold.
	 * @param targetTime key setup time
	 * 
	 * @return number of rounds
	 */
	  public static int determineBestRounds(int targetTime) {
	      byte[] salt = createNewSalt();
	      int plaintext[] = {0x155cbf8e, 0x57f57513, 0x3da787b9, 0x71679d82,
	                         0x7cf72e93, 0x1ae25274, 0x64b54adc, 0x335cbd0b};
	      final byte[] password = {1, 2, 3, 4, 5, 6, 7, 8};
	      BCrypt bcrypt = new BCrypt();

	      /* Define number of test rounds here. Choose a value such that the interval
	       * is significantly greater than the clock resolution, but also significantly
	       * less than the target overall digest time (default 1000ms).
	       * Around 20-50 ms is ideal.
	       */
	      int testrounds = 6;
	      
	      // Calculate the time to create a cipher key with the specified rounds, in msecs.
	      // Do it a number of times and take the average.
	      final long start = System.currentTimeMillis();
	      bcrypt.crypt_raw(password, salt, testrounds, plaintext);
	      bcrypt.crypt_raw(password, salt, testrounds, plaintext);
	      final long Ttestrounds = (System.currentTimeMillis() - start) / 2;
	      
	      // If Tm is the time in msecs to create the key with m rounds, then
	      // the time Tn to calculate the key using n rounds (n > m) is:
	      //
	      //   Tn = 2^(n - m) * Tm
	      //
	      // where we want Tn to be less than 900 msecs.  Solving for n gives:
	      //
	      //   Tn = 2^(n - m) * Tm < 900
	      //   n - m + log2(Tm) < log2(900)
	      //   n < m + log2(900) - log2(Tm)          -- solve for n
	      //   n < m + ln(900)/ln(2) - ln(Tm)/ln(2)  -- convert to natural logs 
	      //
	      // The best number of rounds is the floor of n.
	      final double n = testrounds + (Math.log(targetTime) - Math.log(Ttestrounds)) / Math.log(2);
	      int rounds = (int) n;

	      logger.log(Level.FINE, "determineBestRounds: time for " + testrounds + " rounds - " + Ttestrounds + " ms, calculated rounds - " + rounds);
	      // Make sure rounds does not exceed its valid range.
	      if (rounds < 4) {
	        rounds = 4;
	      } else if (rounds > 31) {
	        rounds = 31;
	      }

	      return rounds;
	}
	  
	  /**
	   * Check the length of the encryption key that will be used.
	   * @return 0 = 256 bit and property was 256 bit (or none)
	   *          1 = 128 bit and property was 256 bit (or none)
	   *          2 = 256 bit and property was 128 bit
	   *          3 = 128 bit and property was 128 bit
	   */
	  public static int checkEncryptionKeyLength() {
	     int retval = 0;
	     try {
	        int maxKeyLengthAllowed = Math.min(Cipher.getMaxAllowedKeyLength(KEY_FACTORY), 256);
	        logger.log(Level.FINE, "checkEncryptionKeyLength: max key length allowed by system is " + maxKeyLengthAllowed);
	        /* get key length property */
	        int keyLengthProperty = Integer.valueOf(SecretsProperties.getInstance().getProperty(Constants.KEYLENGTH));
	        logger.log(Level.FINE, "checkEncryptionKeyLength: key length property is " + keyLengthProperty);
	        if (maxKeyLengthAllowed == 256) {
	           if (keyLengthProperty == 128) retval = 2;
	        } else {
	           if (keyLengthProperty == 256) retval = 1;
	           else retval = 3;
	        }
	     } catch (NoSuchAlgorithmException e) {
	        String msg = "Error checking cipher key length - " + e;
	        logger.log(Level.SEVERE, msg);
	        throw new RuntimeException(msg);
	     }
	     return retval;
	  }

	/**
	 * Create a pair of encryption and decryption ciphers based on the given
	 * password string. The string is not stored internally. This function needs
	 * to be called before calling getEncryptionCipher() or
	 * getDecryptionCipher().
	 * 
	 * @param password
	 *            String to use for creating the ciphers.
	 * @param cipherParms
	 * @return CipherInfo structure with information about the created ciphers.
	 */
	public static CipherInfo createCiphers(byte[] password, CipherParms cipherParms) {
		CipherInfo info = new CipherInfo();
		
		byte[] passwordWithDelim = new byte[password.length+1];
		System.arraycopy(password, 0, passwordWithDelim, 0, password.length);
		passwordWithDelim[password.length] = '\000';
		
		info.parms = new CipherParms(cipherParms);
		final long start = System.currentTimeMillis();		
		try {
			if (info.parms.salt == null || info.parms.rounds == 0) {
		      int keySetupTime = 900;
		      try {
		         keySetupTime = Integer.parseInt(SecretsProperties.getInstance().getProperty(Constants.KEY_SETUP_TIME));
		      } catch (NumberFormatException e) {
		         try {
		            keySetupTime = Integer.parseInt(SecretsProperties.getInstance()
		                        .getDefaultProperty(Constants.KEY_SETUP_TIME));
		         } catch (NumberFormatException e1) {}
		      }
		      logger.log(Level.FINE, "Using key setup time of " + keySetupTime);
			   info.parms.salt = createNewSalt();
			   info.parms.rounds = determineBestRounds(keySetupTime);
			}

			/* 
			 * The max key length that can be used is controlled by the jurisdiction policy.
			 * A 256 bit key cannot be used unless the unlimited strength policy is in force.
			 */
			
			/* Determine the keylen to use...
			 * If the keylen is provided in the cipher parms, use that.
			 * Otherwise use max( maximum allowed by the system, max allowed by PCSecrets).
			 * Set both these values as props so they can be shown inProperties.
			 */

			int plaintext[] = { 0x155cbf8e, 0x57f57513, 0x3da787b9, 0x71679d82, 0x7cf72e93, 0x1ae25274, 0x64b54adc,
					0x335cbd0b };
			SecretsProperties props = SecretsProperties.getInstance();
			
			// get max key length allowed by security policy
			int maxKeyLengthSystem = Cipher.getMaxAllowedKeyLength(KEY_FACTORY);
			logger.log(Level.FINE, "createCiphers: max key length allowed by system is " + maxKeyLengthSystem);
			// get max allowed by PCSecrets
			int keyLengthMax = Integer.valueOf(props.getDefaultProperty(Constants.MAXKEYLENGTH));
			logger.log(Level.FINE, "createCiphers: max key length set to " + keyLengthMax);
			int currentKeyMax = Math.min(maxKeyLengthSystem, keyLengthMax);
			// record current key length max
			props.updateProperty(Constants.MAXKEYLENGTH, String.valueOf(currentKeyMax));
			
			if (info.parms.keylen < 1) { // = not specified
			   info.parms.keylen = currentKeyMax;
			}
			// record current key length
			props.updateProperty(Constants.KEYLENGTH, String.valueOf(info.parms.keylen));
			logger.log(Level.FINE, "createCiphers: current key length is " + info.parms.keylen);
			if (info.parms.keylen < (plaintext.length * 32)) { // keylength assumed to be a multiple of 32
			   plaintext = Arrays.copyOf(plaintext, info.parms.keylen/32); // shorten the key
			}
			
			// generate the ciphers
			BCrypt bcrypt = new BCrypt();
			byte[] rawBytes = bcrypt.crypt_raw(passwordWithDelim, info.parms.salt, info.parms.rounds, plaintext);
            SecretKeySpec spec = new SecretKeySpec(rawBytes, KEY_FACTORY);
            // For backwards compatibility with secrets created on Android M and
            // earlier, create an initial vector of all zeros.
            IvParameterSpec params = new IvParameterSpec(new byte[16]);

            info.encryptCipher = Cipher.getInstance(CIPHER_FACTORY);
            info.encryptCipher.init(Cipher.ENCRYPT_MODE, spec, params);

			info.decryptCipher = Cipher.getInstance(CIPHER_FACTORY);
			info.decryptCipher.init(Cipher.DECRYPT_MODE, spec, params);
		} catch (Exception ex) {
		   String msg = "Error creating ciphers - " + ex;
			logger.log(Level.SEVERE, msg, ex);
			throw new RuntimeException(msg);
		}
		logger.log(Level.FINE, "createCiphers: time to create ciphers for " + info.parms.rounds + " rounds : " + (System.currentTimeMillis() - start) + "ms");
		return info;
	}

	/**
	 * Gets the salt and rounds from the input stream, skipping the 4 byte
	 * signature.
	 * 
	 * The input stream begins:
	 *   version (1)
	 *   signature bytes (4)
	 *   keylength (2)
	 *   salt length (1)
	 *   salt (16)
	 *   rounds (1)
	 * 
	 * @param input
	 *            The stream to read the salt and rounds from.
	 * @return salt and rounds data
	 * @throws IOException
	 */
	public static CipherParms getCipherParms(InputStream input) throws IOException {
		byte[] signature = new byte[SIGNATURE.length];
		int keylen = 0;
		byte[] salt = null;
		int rounds = 0;
		
        logger.log(Level.INFO, "getCipherParms");
        input.reset();
		input.read(); // version, not currently used
		input.read(signature); // signature bytes
		if (Arrays.equals(signature, SIGNATURE)) {
		   // read the key length as a 2 byte integer
		   keylen = input.read() * 256;
		   keylen += input.read();
		   
			int length = input.read(); // salt length
			salt = new byte[length]; // salt
			input.read(salt);
			rounds = input.read(); // rounds
			
			logger.log(Level.FINE, "getCipherParms: keylen " + keylen + ", salt " + Bytes.byteArrayToHex(salt)
	                  + ", rounds " + rounds);
		} else {
         logger.log(Level.WARNING,
                     "getCipherParms: invalid security header signature; expected " + Bytes.byteArrayToHex(SIGNATURE)
                                 + ", got " + Bytes.byteArrayToHex(signature));
         return null;
		}
		
		return new CipherParms(keylen, salt, rounds);
	}
	
	/**
	 * Write keylen, salt and rounds data to the supplied output stream
	 * 
	 * Security header consist of:
	 *   version (1)
	 *   signature bytes (4)
    *   keylength (2)
    *   salt length (1)
    *   salt (16)
    *   rounds (1)
    *   
    * Total length = 25
	 * 
	 * @param parms 
	 * @param os
	 * @throws IOException
	 */
	public static void writeSecurityHeader(CipherParms parms, OutputStream os) throws IOException {
	   final int CURRENT_HEADER_VERSION = 1;
	   os.write(CURRENT_HEADER_VERSION);
		os.write(SIGNATURE); // write signature bytes
		// write the key length as a 2 byte integer
		byte[] keylenBytes = new byte[2];
		keylenBytes[0] = (byte)(parms.keylen >> 8);
		keylenBytes[1] = (byte)parms.keylen;
		os.write(keylenBytes);
		
		os.write(parms.salt.length); // write salt length
		os.write(parms.salt); // write salt
		os.write(parms.rounds); // write rounds
	}
	
	/**
	 * For testing only
	 * 
	 * @param args
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException {

	    long start = System.currentTimeMillis();
        checkBCProvider();
        SecretsProperties.getInitialInstance(".", true);
	    
	    try {
	    	CipherInfo cipherInfo = createCiphers("secretstring".getBytes("UTF-8"), null);
	    	
			// Our cleartext
			byte[] cleartext = "This is another example         ".getBytes();
			System.out.println("Clear string: " + Strings.toHex(cleartext));

			// Encrypt the cleartext
			byte[] ciphertext = cipherInfo.encryptCipher.doFinal(cleartext);
			long time = System.currentTimeMillis() - start;
			
			System.out.println("Encrypted string: " + Strings.toHex(ciphertext));
			System.out.println("Time taken: " + time + " ms");
			
			start = System.currentTimeMillis();
			byte[] decryptedtext = cipherInfo.decryptCipher.doFinal(ciphertext);
			time = System.currentTimeMillis() - start;
			System.out.println("Decrypted string: " + new String(decryptedtext));
			System.out.println("Time taken: " + time + " ms");
			
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
