package dataquality;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

public class BuyerObtainKGPart {
	
	private static IvParameterSpec iv;
//	private static LinkedList<byte[]> encryptedParts = null;
	
	public static LinkedList<Model> obtainKGPart(LinkedList<BigInteger> keysBI, byte[] byteIV, LinkedList<byte[]> encryptedParts)
	{
		Logger logger = Log.getLogger();
		
		LinkedList<Model> obtainedModels = new LinkedList<Model>();
		iv = new IvParameterSpec(byteIV);
		
		logger.info("Start trying to decrypt model parts.");
		// Try the key on each encrypted part to find the one it fits
		for(byte[] part : encryptedParts)
		{
			// Try each key on the string
			for(BigInteger keyBI : keysBI)
			{
				// ------------ Recreate key from BigIntger --------------------------
				try {
					SecretKeySpec secretKey = SellerPrepareKG.bigIntegerToAESKey(keyBI);
					
					// --------- Try to decrypt the string ---------------------------
					String decryptedString = decrypt(part, secretKey, iv);
		            
		            // Try to turn decrypted string into model (only works for the String the Buyer obtained the key for)
		            Model decrypted = ModelTools.stringToModel(decryptedString);
		            // If the String could be turned into a model add it to the found models
		            if(decrypted != null)
		            {
		            	obtainedModels.add(decrypted);
		            }
				} catch (NoSuchAlgorithmException e) 
				{
					logger.info("Exception in oblivious transfer, when trying to decrypt model parts. \n" 
							+ e.getMessage() + "\n"
							+ e.toString() + "\n"
							+ "Protocol is being terminated.");
					System.exit(1);
				}
			}
		}
		logger.info("Done trying to decrypt model parts.");
        return obtainedModels;
	}
	
	
	
	public static String decrypt(byte[] encrypted, SecretKeySpec secretKey, IvParameterSpec iv)
	{
		String decryptedString = "";
		
		try {
			// Specify AES mode and padding
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			decryptedString = new String(cipher.doFinal(encrypted), "UTF-8");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException e) 
		{

		}
		
		return decryptedString;
	}
	
	public static IvParameterSpec getIV()
	{
		return iv;
	}
}
