package tests;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.logging.Logger;

import oblivioustransfer.BuyerOT;
import oblivioustransfer.SellerOT;
import privatesetintersection.Alice;
import protocol.Log;

/**
 * Class containing tests regarding Oblivious Transfer
 * 
 * @author ---
 *
 */
public class TestObliviousTransfer 
{
	
	/**
	 * Tests Oblivious Transfer
	 * Generates random messages.
	 * Runs OT on them.
	 * 
	 * @param n number of messages
	 * @param k number of messages to obtain
	 * @return true if all messages where received, false otherwise
	 */
	public static boolean obliviousTransferTest(int n, int k)
	{
		Logger logger = Log.getLogger();
		LinkedList<BigInteger> messages = new LinkedList<BigInteger>();
		try 
		{
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			for(int i = 0; i < n; i++)
			{
				BigInteger message = new BigInteger(2048, secureRandom);
				messages.add(message);
			}
		} catch (NoSuchAlgorithmException e) {
			logger.info("NoSuchAlgorithmException. Likely some problem with SecureRandom.");
			e.printStackTrace();
		}
		
		KeyPair keyPair = Alice.produceKeyPair();
		LinkedList<BigInteger> randomMessages = SellerOT.sellerOT(messages, keyPair);
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		LinkedList<BigInteger> vValues = BuyerOT.buyerOT(k, publicKey, randomMessages);
			
		LinkedList<LinkedList<BigInteger>> kmprime = SellerOT.sellerOTPart2(messages, k, vValues, randomMessages);
		LinkedList<BigInteger> mbs = BuyerOT.buyerOTPart2(publicKey, kmprime);
		
		logger.info("Able to decode all messages: " + messages.containsAll(mbs) + "\n"
				+ "Received " + mbs.size() + " messages. k was " + k);

		return messages.containsAll(mbs);
	}

}
