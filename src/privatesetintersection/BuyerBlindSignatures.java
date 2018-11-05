package privatesetintersection;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import communication.Client;
import protocol.Log;
import protocol.Tuple;

public class BuyerBlindSignatures
{
	private static HashMap<Long, BigInteger> blindingMap;
	private static RSAPublicKey publicKey;
	
	/**
	 * Computes blind signatures for all elements contained in a HashMap
	 * @param numberStatementMap containing elements to be signed and a long which identifies each element
	 * @return LinkedList of tuples containing blind signatures concatenated with a String representation of the signed element as x and the identifier of the original element as y
	 */
	public static <K> LinkedList<Tuple<String,Long>> bobBlindSignature (HashMap<Long, K> numberStatementMap)
	{
		Logger logger = Log.getLogger();
		logger.info("Start getting signatures.");
		
		LinkedList<Tuple<K, Long>> statementNumberList = hashmapToTupleList(numberStatementMap);
		LinkedList<Tuple<BigInteger, Long>> blindedStatements = blindStatements(statementNumberList);
		
		// Communication
		Client client = Client.getClient();
		client.sendObject(blindedStatements);
		LinkedList<Tuple<BigInteger, Long>> blindedSignatures = client.<LinkedList<Tuple<BigInteger, Long>>>readObject();
		
		LinkedList<Tuple<String, Long>> unblindedSignatures = unblindStatements(blindedSignatures, numberStatementMap);
		
		logger.info("Done obtaining signatures.");
		return unblindedSignatures;
	}
	
	/**
	 * Blinds given elements
	 * @param statementNumberTuples List of Tuples elements and identifiers
	 * @return blinded elements with identifiers
	 */
	private static <K> LinkedList<Tuple<BigInteger, Long>> blindStatements (LinkedList<Tuple<K, Long>> statementNumberTuples)
	{
		Logger logger = Log.getLogger();
		logger.info("Start blinding Buyer elements.");
		
		// List of blinded statements
		LinkedList<Tuple<BigInteger, Long>> blindedStatements = new LinkedList<Tuple<BigInteger, Long>>();
		
		blindingMap = new HashMap<Long, BigInteger>(statementNumberTuples.size());
		
		ExecutorHandling.calculateParallelForListElements(statementNumberTuples, (statementTuple) -> 
		{
			Tuple<BigInteger, BigInteger> mu = Bob.calculateMu(statementTuple.x.toString(), publicKey);
			// Return blinded statement
			return new Tuple<Tuple<BigInteger, Long>, BigInteger>(new Tuple<BigInteger, Long>(mu.x, statementTuple.y), mu.y);
		}, (Future future) -> 
		{
			Tuple<Tuple<BigInteger, Long>, BigInteger> f;
			try {
				f = (Tuple<Tuple<BigInteger, Long>, BigInteger>)future.get();
				blindedStatements.add(f.x);
				blindingMap.put(f.x.y, f.y);
			} catch (InterruptedException | ExecutionException e) {
				logger.info("Exception in BuyerBlindSignatures blindStatements, when trying to read features of parallel computation. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
				System.exit(1);
			}
		});
		
		logger.info("Done blinding Elements. Blinded singatures: " + blindedStatements.size());
		
		return blindedStatements;
	}
	
	/**
	 * Removes blinding from signed elements and concatenates the signature to the string of the original element
	 * @param signedStatements signed elements with identifier
	 * @param numberStatementMap mapping from element to identifier
	 * @return unblinded elements
	 */
	private static <K> LinkedList<Tuple<String, Long>> unblindStatements(LinkedList<Tuple<BigInteger, Long>> signedStatements, HashMap<Long, K> numberStatementMap)
	{
		Logger logger = Log.getLogger();
		logger.info("Start unblinding signatures.");
		
		BigInteger N = publicKey.getModulus();

		// List of unblinded signatures
		LinkedList<Tuple<String, Long>> signatures = new LinkedList<Tuple<String, Long>>();
		
		ExecutorHandling.calculateParallelForListElements(signedStatements, (statement) -> 
		{
			String signature = Bob.signatureCalculation(statement.x, N, blindingMap.get(statement.y));
			
			if(!Bob.verify(signature, publicKey, numberStatementMap.get(statement.y).toString()))
			{
				logger.info("Signature failed!");
			}
			
			// -------------- Hash origininal statement and signature together (like in the paper) ---------------
			String statementWithSignature = numberStatementMap.get(statement.y).toString() + signature;
			String result = DigestUtils.sha512Hex(statementWithSignature);
			//----------------------------------------------------------------------------------------------------
			
			return new Tuple<String, Long>(result, statement.y);
		}, (Future future) -> 
		{
			try {
				signatures.add((Tuple<String, Long>) future.get());
			} catch (InterruptedException | ExecutionException e) {
				logger.info("Exception in BuyerBlindSignatures unblindStatements, when trying to read features of parallel computation. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
				System.exit(1);
			}
		});
		
		logger.info("Done unblinding signatures. Unblinded Singatures: " + signatures.size());

		return signatures;
	}
	
	public static void setPublicKey(RSAPublicKey key)
	{
		publicKey = key;
	}
	
	/**
	 * Turns HashMap into Linked List of tuples where the keys are y and the values are x
	 * @param numberStatementMap mapping from identifier to original element
	 * @return LinkedList containing elements of HashMap as tuples
	 */
	public static <K> LinkedList<Tuple<K, Long>> hashmapToTupleList(HashMap<Long, K> numberStatementMap)
	{
		LinkedList<Tuple<K,Long>> list = new LinkedList<Tuple<K,Long>>();
		
		for(HashMap.Entry<Long, K> entry : numberStatementMap.entrySet())
		{
			list.add(new Tuple<K, Long>(entry.getValue(), entry.getKey()));
		}
		
		return list;
	}
}
