package oblivioustransfer;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.logging.Logger;

import protocol.Log;

public class SellerOT 
{
	private static KeyPair alicePair;  //Alice key pair
	private static RSAPrivateCrtKey alicePrivate; // Alice private key d
	private static RSAPublicKey alicePublic; // Alice public key e
	private static BigInteger N; // Key pair's modulus
	private static BigInteger d; // private exponent d
	
	/**
	 * Oblivious Transfer Alice Part 1
	 * Generates random messages that need to be send to Bob.
	 * @param messages / secrets
	 * @param keyPair to be used
	 * @return random messages generated and needed in second part
	 */
	public static LinkedList<BigInteger> sellerOT(LinkedList<BigInteger> messages, KeyPair keyPair)
	{
		Logger logger = Log.getLogger();
		logger.info("Start seller OT.");
		
		setKey(keyPair);
		
		// ---------------- Generate random messages --------------------------------
		LinkedList<BigInteger> randomMessages = new LinkedList<BigInteger>();
		try 
		{
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			for(int i = 0; i < messages.size(); i++)
			{
				BigInteger randomMessage = new BigInteger(2048, secureRandom);
				randomMessages.add(randomMessage);
			}
		} catch (NoSuchAlgorithmException e) 
		{
			logger.info(e.getMessage());
			logger.info(e.toString());
			e.printStackTrace();
			System.exit(1);
		}
		return randomMessages;
	}
	
	/**
	 * Oblivious Transfer Alice Part 2
	 * From choice of Bob subtracts random message and adds the message.
	 * This has to be done for all combinations.
	 * @param messages / secrets
	 * @param amount how many messages / secrets are to be shared with Bob.
	 * @param vValues obscured choices of Bob. (Obscured random message from first part)
	 * @param randomMessages that where send to Bob in part 1
	 * @return obscured messages where Bob can crack those he chose earlier but no others
	 */
	public static LinkedList<LinkedList<BigInteger>> sellerOTPart2(LinkedList<BigInteger> messages, int amount, LinkedList<BigInteger> vValues, LinkedList<BigInteger> randomMessages)
	{
		Logger logger = Log.getLogger();
		
		if(vValues.size() > amount)
		{
			logger.info("Buyer tried to obtain more secrets then agreed upon. Terminating protocol.");
			System.exit(1);
		}
		
		LinkedList<LinkedList<BigInteger>> kmessagesprime = new LinkedList<LinkedList<BigInteger>>();
		
		for(BigInteger v : vValues)
		{
			LinkedList<BigInteger> blindingsK = new LinkedList<BigInteger>();
			for(BigInteger randomMessage : randomMessages)
			{
				BigInteger k = (v.subtract(randomMessage)).modPow(d, N);
				blindingsK.add(k);
			}
			
			int i = 0;
			LinkedList<BigInteger> messagesprime = new LinkedList<BigInteger>();
			for(BigInteger message : messages)
			{
				messagesprime.add(message.add(blindingsK.get(i)).mod(N));
				i++;
			}
			
			kmessagesprime.add(messagesprime);
		}
		logger.info("Done seller OT.");
		return kmessagesprime;
	}
	
	private static void setKey(KeyPair keyPair) 
	{
		alicePair = keyPair;
		alicePrivate = (RSAPrivateCrtKey) alicePair.getPrivate(); //get the private key d out of the key pair Alice produced
		alicePublic = (RSAPublicKey) alicePair.getPublic(); //get  the public key e out of the key pair Alice produced
		N = alicePublic.getModulus(); //get the modulus of the key pair produced by Alice
		d = alicePrivate.getPrivateExponent(); //get private exponent d
	}
}
