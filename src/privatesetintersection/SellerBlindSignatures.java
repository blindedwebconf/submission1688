package privatesetintersection;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

import protocol.Log;
import protocol.Tuple;

public class SellerBlindSignatures {
	
	private static boolean existingKey = false;
	private static KeyPair alicePair;  				//alice key pair
	private static RSAPrivateCrtKey alicePrivate; 	// alice private key d
	private static RSAPublicKey alicePublic; 		// alice public key e
	private static BigInteger N; 					// Key pair's modulus
	private static BigInteger P; 					// prime number p used to produce the key pair
	private static BigInteger Q; 					// prime number q used to produce the key pair
	private static BigInteger PinverseModQ; 		// p inverse modulo q
	private static BigInteger QinverseModP; 		// q inverse modulo p
	private static BigInteger d; 					// private exponent d
	
	/**
	 * Signs statements of a model.
	 * Concatenates string representing statement with the signature.
	 * @param model containing statements to be signed
	 * @return List of signatures
	 */
	public static LinkedList<String> aliceBlindSignatures(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start signing Seller statements.");
		
		// If the key pair has not been created yet by a different step of the protocol
		// Set up key pair for blind signatures
		if(!existingKey)
		{
			createKeyPair();
		}
		
		//------------ Generate signature for each statement ----------------
		// An iterator over all statements
		List<Statement> statements = model.listStatements().toList();
		logger.info("Finished creating statement list.");
		
		LinkedList<String> signatures = new LinkedList<String>();
		
		ExecutorHandling.calculateParallelForListElements(statements, (statement) -> 
		{
			Tuple<BigInteger,BigInteger> muR = Bob.calculateMu(statement.toString(), alicePublic); //call Bob's function calculateMu with alice Public key as input in order to calculate mu, and store it in mu variable

			BigInteger muprime = Alice.calculateMuPrimeWithChineseRemainderTheorem(muR.x, d, N, P, Q, PinverseModQ, QinverseModP); // call Alice's function calculateMuPrime with mu produced earlier by Bob as input, to calculate  mu' and store it to muprime  variable

			String sig = Bob.signatureCalculation(muprime, N, muR.y); // call Bob's function signatureCalculation with muprime as input and calculate the signature, then store it in sig variable
			
			// -------------- Hash origininal statement and signature together (like in the paper) ---------------
			String statementWithSignature = statement.toString() + sig;
			String result = DigestUtils.sha512Hex(statementWithSignature);
			//----------------------------------------------------------------------------------------------------
			
			return result;
		}, (Future future) -> 
		{
			try {
				signatures.add((String) future.get());
			} catch (InterruptedException | ExecutionException e) {
				logger.info("Exception in SellerPSI.PrivateSetIntersection, when trying to sign Sellers statements. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
				System.exit(1);
			}
		});

		logger.info("Done signing Statements. Number of Statements signed: " + signatures.size());
		return signatures;
	}
	
	/**
	 * Creates a RSA key pair
	 * @return RSA key pair
	 */
	public static KeyPair createKeyPair()
	{	
		Logger logger = Log.getLogger();
		
		alicePair = Alice.produceKeyPair(); 						// call Alice's function to produce a key pair (N, e ,d), and save it in alicePair variable
		alicePrivate = (RSAPrivateCrtKey) alicePair.getPrivate(); 	//get the private key d out of the key pair Alice produced
		alicePublic = (RSAPublicKey) alicePair.getPublic(); 		//get  the public key e out of the key pair Alice produced
		N = alicePublic.getModulus(); 								//get the modulus of the key pair produced by Alice
		P = alicePrivate.getPrimeP(); 								//get the prime number p used to produce the key pair
    	Q = alicePrivate.getPrimeQ(); 								//get the prime number q used to produce the key pair
    	
        //We split the mu^d modN in two , one mode p , one mode q
        PinverseModQ = P.modInverse(Q); //calculate p inverse modulo q
        QinverseModP = Q.modInverse(P); //calculate q inverse modulo p

        d = alicePrivate.getPrivateExponent(); //get private exponent d
        existingKey = true;
        
        logger.info("A RSA keypair has been created.");
		return alicePair;
	}
	
	
	/**
	 * Signs blinded elements of Bob.
	 * @param blindedStatements blinded elements to be singed with identifier.
	 * @return List of tuples containing signatures and identifiers. 
	 */
	public static LinkedList<Tuple<BigInteger, Long>> signBuyerStatements(LinkedList<Tuple<BigInteger, Long>> blindedStatements)
	{
		Logger logger = Log.getLogger();
		logger.info("Start signing Buyer statements.");
		//List of signed statements
		LinkedList<Tuple<BigInteger, Long>> signedStatements = new LinkedList<Tuple<BigInteger, Long>>();
		
		ExecutorHandling.calculateParallelForListElements(blindedStatements, (statement) -> 
		{
			BigInteger muprime = Alice.calculateMuPrimeWithChineseRemainderTheorem(statement.x, d, N, P, Q, PinverseModQ, QinverseModP); // call Alice's function calculateMuPrime with mu produced earlier by Bob as input, to calculate  mu' and store it to muprime  variable
			// Return blinded statement
			return new Tuple<BigInteger,Long>(muprime, statement.y);
		}, (Future future) -> 
		{
			try {
				signedStatements.add((Tuple<BigInteger, Long>) future.get());
			} catch (InterruptedException | ExecutionException e) {
				logger.info("Exception in SellerBlindSignatures signBuyerStatements, when trying to read features of parallel computation. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
				System.exit(1);
			}
		});
		
		logger.info("Done signing Buyer statements. Signed Statements: " + signedStatements.size());

		return signedStatements;
	}
	
	/**
	 * Calculates signatures for a LinkedList of Tuples. Signs tuple.x and matches signature with tuple.y
	 * @param multisetIndexList
	 * @return List of Tuples. Tuple.x is signature, Tuple.y is original Tuple.y
	 */
	public static <K> LinkedList<Tuple<Long,String>> signSellerMultiset(LinkedList<Tuple<K,Long>> multisetIndexList)
	{
		Logger logger = Log.getLogger();
		
		logger.info("Start signing Seller Multiset.");
		
		if(!existingKey)
		{
			createKeyPair();
		}
		
		LinkedList<Tuple<Long,String>> signatures = new LinkedList<Tuple<Long,String>>();
		
		ExecutorHandling.calculateParallelForListElements(multisetIndexList, (element) ->
		{
			Tuple<BigInteger,BigInteger> muR = Bob.calculateMu(element.x.toString(), alicePublic); //call Bob's function calculateMu with alice Public key as input in order to calculate mu, and store it in mu variable

			BigInteger muprime = Alice.calculateMuPrimeWithChineseRemainderTheorem(muR.x, d, N, P, Q, PinverseModQ, QinverseModP); // call Alice's function calculateMuPrime with mu produced earlier by Bob as input, to calculate  mu' and store it to muprime  variable

			String sig = Bob.signatureCalculation(muprime, N, muR.y); // call Bob's function signatureCalculation with muprime as input and calculate the signature, then store it in sig variable

			// -------------- Hash origininal statement and signature together (like in the paper) ---------------
			String statementWithSignature = element.x.toString() + sig;
			String result = DigestUtils.sha512Hex(statementWithSignature);
			//----------------------------------------------------------------------------------------------------
			
			return new Tuple<Long,String> (element.y, result);
		}, (Future future) -> 
		{
			try {
				signatures.add((Tuple<Long,String>) future.get());
			} catch (InterruptedException | ExecutionException e) {
				logger.info("Exception in SellerBlindSignatures signSellerMultiset, when trying to read features of parallel computation. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
				System.exit(1);
			}
		});
		
		logger.info("Done signing Sellers Multiset. Number of elements signed: " + signatures.size());
		
		return signatures;
	}
	
	/**
	 * Sets key to be used for signatures. 
	 * @param keyPair Keypair needs to be for RSA.
	 */
	public static void buyerTestSetKey(KeyPair keyPair)
	{
		existingKey = true;
		alicePair = keyPair;  									//alice key pair
		alicePrivate = (RSAPrivateCrtKey) keyPair.getPrivate(); // alice private key d
		alicePublic = (RSAPublicKey) keyPair.getPublic(); 		// alice public key e
		N = alicePublic.getModulus(); 							// Key pair's modulus
		P = alicePrivate.getPrimeP(); 							// prime number p used to produce the key pair
		Q = alicePrivate.getPrimeQ(); 							// prime number q used to produce the key pair
		PinverseModQ = P.modInverse(Q); 						//calculate p inverse modulo q
        QinverseModP = Q.modInverse(P); 						//calculate q inverse modulo p
        d = alicePrivate.getPrivateExponent(); 					//get private exponent d
	}
}
