package oblivioustransfer;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.logging.Logger;

import protocol.Log;

public class BuyerOT 
{
	private static LinkedList<Integer> bValues = new LinkedList<Integer>();
	private static LinkedList<BigInteger> kValues = new LinkedList<BigInteger>();
	
	/**
	 * Oblivious transfer from Bobs perspective.
	 * In this first step Bob chooses which secrets he would like to receive and obscures his choice.
	 * @param amount of secrets to obtain
	 * @param publicKey of Alice
	 * @param randomMessages created by Alice
	 * @return choices of Bob
	 */
	public static LinkedList<BigInteger> buyerOT(int amount, RSAPublicKey publicKey, LinkedList<BigInteger> randomMessages)
	{
		Logger logger = Log.getLogger();
		logger.info("Start Buyer OT.");
		
		bValues.clear();
		kValues.clear();
		
		BigInteger N = publicKey.getModulus();
		BigInteger e = publicKey.getPublicExponent();
		
		// --------- Checking Parameters ----------------------
		if(amount == 0)
		{
			logger.info("Warning: Oblivious Transfer Buyer. Strange parameter. Buying 0 secrets makes no sense.");
			return null;
		}
		if(amount > randomMessages.size())
		{
			logger.info("Warning: Oblivous Transfer Buyer. Trying to buy more parts of the graph then exist. Getting all parts of the KG.");
			amount = randomMessages.size();
		}
		
		// --------- Creating secure random -------------------
		SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) 
		{
			logger.info("Exception in Buyer Oblivious Transfer, when trying to get a SecureRandom instance SHA1PRNG. \n" 
					+ e1.getMessage() + "\n"
					+ e1.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
		// ---------------- Generate and obscure choice -------------------
		logger.info("Start obscuring choice.");
		LinkedList<BigInteger> vValues = new LinkedList<BigInteger>();
		for(int i = 0; i < amount; i++)
		{
			BigInteger k = new BigInteger(2048, secureRandom);
			int b = secureRandom.nextInt(randomMessages.size());
			
			BigInteger xb = randomMessages.get(b);
			BigInteger v = xb.add(k.modPow(e, N)).mod(N);
			
			kValues.add(k);
			bValues.add(b);
			vValues.add(v);
		}
		logger.info("Done obscuring choice.");
		
		return vValues;
	}
	
	/**
	 * Oblivious Transfer part 2 from Bobs side.
	 * Uses information from Alice (based on his choices) to decrypt secrets.
	 * @param publicKey of Alice
	 * @param kmprime information from Alice calculated on earlier choices.
	 * @return obtained secrets
	 */
	public static LinkedList<BigInteger> buyerOTPart2(RSAPublicKey publicKey, LinkedList<LinkedList<BigInteger>> kmprime)
	{
		Logger logger = Log.getLogger();
		
		BigInteger N = publicKey.getModulus();
		
		logger.info("Start determining secrets.");
		LinkedList<BigInteger> secrets = new LinkedList<BigInteger>();
		for(int i = 0; i < kmprime.size(); i++)
		{
			LinkedList<BigInteger> mprime = kmprime.get(i);
			int b = bValues.get(i);
			BigInteger k = kValues.get(i);
			BigInteger mbprime = mprime.get(b);
			BigInteger mb = mbprime.subtract(k).mod(N);
			secrets.add(mb);
		}
		logger.info("Done determining secrets.");
		logger.info("Done with Buyer OT.");
		return secrets;
	}
}
