package dataquality;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.jena.rdf.model.Model;

import protocol.Log;
import protocol.ModelTools;

public class SellerPrepareKG {
	
	/**
	 * Takes a partitioning of a model (list of models) and encrypts each part with AES
	 * @param partitioning
	 * @return Class containing encrypted parts, keys, Initialization Vector
	 */
	public static EncryptionStorage prepareKG(LinkedList<Model> partitioning)
	{
		Logger logger = Log.getLogger();
		
		// ----------------- Turn partitions into Strings -----------------
		logger.info("Start encoding partition parts as Strings.");
		LinkedList<String> partitionsAsStrings = new LinkedList<String>();
		for(Model part : partitioning)
		{
			partitionsAsStrings.add(ModelTools.modelToString(part));
		}
		logger.info("Done encoding partition parts as Strings.");
		
		// ----------------- Encrypt partition Strings with AES -------
		logger.info("Start encrypting partition Strings with AES.");
		byte[] byteIV = new byte[16];
		// Get a instance of SecureRandom
		SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.nextBytes(byteIV);
		} catch (NoSuchAlgorithmException e1) {
			logger.info(e1.getMessage());
			logger.info(e1.toString());
			e1.printStackTrace();
			System.exit(1);
		}
		IvParameterSpec iv = new IvParameterSpec(byteIV);
		// List to store the encrypted strings
		LinkedList<byte[]> encryptedParts = new LinkedList<byte[]>();
		// List to store the BigIntegers used to generate keys which are used to encrypt the strings (one for each string)
		LinkedList<BigInteger> keys = new LinkedList<BigInteger>();
		// For each string generate a new key and encrypt it with AES
		for(String partAsS : partitionsAsStrings)
		{
			try {
				// ----------- Generate AES Key ---------------------------------
				// Use Secure Random to generate a random BigInteger
				BigInteger keyBI = new BigInteger(1024, secureRandom);
				
				SecretKeySpec secretKey = bigIntegerToAESKey(keyBI);

				// ----------- Encrypt the String -------------------------------
				// Get an instance, Specify AES, mode, and padding
				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
				// Specify that it will be used to encrypt and the key to be used
	            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
	            // Encrypt and turn the result into a String
	            byte[] encrypted = cipher.doFinal(partAsS.getBytes("UTF8"));
	            
	            // Add the encrypted String to the encrypted model parts
	            encryptedParts.add(encrypted);
	            // Store the BigInteger used to generate the key
	            // This is shared using OT in order to allow the Buyer to decrypt 1 part of the model
	            keys.add(keyBI);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) 
			{
				logger.info("Exception in oblivious transfer, when trying to encrypt model parts with AES. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
				System.exit(1);
			}
			
		}
		logger.info("Done encrypting partition Strings with AES.");
		
		EncryptionStorage storage = new EncryptionStorage();
		storage.setKeys(keys);
		storage.setByteIV(byteIV);
		storage.setEncryptedParts(encryptedParts);
		
		return storage;
	}
	
	public static SecretKeySpec bigIntegerToAESKey(BigInteger bi) throws NoSuchAlgorithmException
	{
		// Turn BigInteger into a Bytearray
		byte[] keyByte = bi.toByteArray();
		// Hash Bytearray for "more" randomness
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		keyByte = sha.digest(keyByte);
		// Get the first 128 bit to be used as the key
		keyByte = Arrays.copyOf(keyByte, 32); // use only first 256 bit
		// Create the key from the Bytearray
		SecretKeySpec secretKey = new SecretKeySpec(keyByte, "AES");
		
		return secretKey;
	}
	

	
}
