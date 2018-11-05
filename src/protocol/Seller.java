package protocol;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.google.common.hash.BloomFilter;

import communication.Server;
import dataquality.EncryptionStorage;
import dataquality.SellerPrepareKG;
import knowledgegraphpartitioning.Partitioning;
import oblivioustransfer.SellerOT;
import privatesetintersection.KGIntersectionSeller;
import privatesetintersection.SellerBlindSignatures;
import statistics.CBFSeller;
import statistics.Multisets;
import statistics.Statistics;
import statistics.StatisticsResults;

public class Seller 
{
	private static StatisticsResults statisticsResults = null;

	/**
	 * This method gets called if the instance of this protocol is run as seller.
	 * It calls all the steps that the seller needs to do.
	 * 
	 * @param model Model of the sellers knowledge graph
	 */
	public static void runAsSeller(Model model, Scanner scanner)
	{
		Logger logger = Log.getLogger();
		
		logger.info("Enter path to your Keystore.");
		String pathToKeyStore = scanner.nextLine();
		
		logger.info("Enter Keystore password.");
		String keyStorePw = scanner.nextLine();
		
		System.setProperty("javax.net.ssl.keyStore", pathToKeyStore); 
		System.setProperty("javax.net.ssl.keyStorePassword", keyStorePw);
		
		Server server = Server.getServer();
		
		KeyPair keypair = SellerBlindSignatures.createKeyPair();
		RSAPublicKey publicKey = (RSAPublicKey) keypair.getPublic();
		server.sendObject(publicKey);
		
		// ----------- Entropy Buyer signing ----------------
		LinkedList<String> input = new LinkedList<String>();
		boolean runEntropy = GetUserInput.runStep("Calculate the Entropies. [yes, no]");
		if(runEntropy)
		{
			input = entropySignBuyer();
		}
		
		// ----------- Intersection -------------------------
		if(GetUserInput.runStep("Calculate the intersection. [yes, no]"))
		{
			intersectionStep(model);
		}
		GetUserInput.continueProtocol();
		
		// ----------- Entropy and Statistics -------------------------		
		// Entropy
		if(runEntropy)
		{
			entropySeller(model, input);
			
			logger.info("Number of Bytes sent after Entropies: " + server.getOutputStreamCount());
			logger.info("Number of Bytes received after Entropies: " + server.getInputStreamCount());
		}
		GetUserInput.continueProtocol();
		
		// Statistics
		if(GetUserInput.runStep("Calculate the descriptive Statistics. [yes, no]"))
		{
			logger.info("Starting Statistics step.");
			statisticsResults = Statistics.statistics(model);
			server.sendObject(statisticsResults);
			
			// Wait for Buyer to finish as well
			logger.info(server.<String>readObject());
			
			logger.info("Number of Bytes sent after Statistics: " + server.getOutputStreamCount());
			logger.info("Number of Bytes received after Statistics: " + server.getInputStreamCount());
		}
		GetUserInput.continueProtocol();
		
		// ----------- Oblivious Transfer -------------------------
		boolean runOT = GetUserInput.runStep("Run Oblivious Transfer. [yes, no]");
		LinkedList<BigInteger> keys = null;
		if(runOT)
		{
			keys = dataQualityStep(model, keypair);
		}
		GetUserInput.continueProtocol();
		
		// ----------- finally send over the full KG -------------
		logger.info("Start sending model and used keys to the Buyer.");
		server.sendModel(model);
		
		server.sendObject(keypair);
		
		if(runOT)
		{
			server.sendObject(keys);
		}
		
		// Wait for Buyer to finish as well
		logger.info(server.<String>readObject());
		
		logger.info("Number of Bytes sent: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received: " + server.getInputStreamCount());
		
		scanner.close();
	}
	
	/**
	 * Runs the intersection step.
	 * First generates signatures for the sellers statements, then puts them into a Bloom filter.
	 * Then signs the buyers statements.
	 * @param model
	 */
	private static void intersectionStep(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Starting Intersection Step.");
		
		LinkedList<String> signatures = SellerBlindSignatures.aliceBlindSignatures(model);
		
		double fpp = GetUserInput.getBloomFilterFPP();
		BloomFilter<String> bf = KGIntersectionSeller.trainBloomFilter(fpp, signatures);

		Server server = Server.getServer();
		LinkedList<Tuple<BigInteger, Long>> blindedStatements = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
		LinkedList<Tuple<BigInteger, Long>> blindedSignatures = SellerBlindSignatures.signBuyerStatements(blindedStatements);
		server.sendObject(blindedSignatures);
		
		server.sendObject(bf);
		
		logger.info("Done sending BF. Its size was: " + bf.approximateElementCount());
		
		// Wait for Buyer to finish as well
		logger.info(server.<String>readObject());
		
		logger.info("Number of Bytes sent after Intersection: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received after Intersection: " + server.getInputStreamCount());
	}
	
	/**
	 * Sings the entropy multisets of the buyer
	 * @return which entropies are to be computed and have been signed
	 */
	private static LinkedList<String> entropySignBuyer()
	{
		LinkedList<String> input = GetUserInput.entropiesToCompute();
		
		Server server = Server.getServer();
		
		if(input.contains("all") || input.contains("1"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("2"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("3"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("4"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("5"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("6"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("7"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("8"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		if(input.contains("all") || input.contains("9"))
		{
			LinkedList<Tuple<BigInteger, Long>> blindedMultiset = server.<LinkedList<Tuple<BigInteger, Long>>>readObject();
			LinkedList<Tuple<BigInteger, Long>> multisetSignatures = SellerBlindSignatures.signBuyerStatements(blindedMultiset);
			server.sendObject(multisetSignatures);
		}
		
		return input;
	}
	
	/**
	 * Handles the seller side of the entropy computations
	 * @param model of the seller
	 */
	private static void entropySeller(Model model, LinkedList<String> input)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing entropies.");
		
		HashMap<Tuple<Resource, RDFNode>,Integer> descMultisetSeller = null;
		HashMap<Tuple<Resource, RDFNode>,Integer> classifMultisetSeller = null;
		HashMap<Tuple<Resource,RDFNode>,Integer> descmMultisetSeller = null;
		HashMap<Resource,Integer> descmpMultisetSeller = null;
		HashMap<Resource,Integer> econnMultisetSeller = null;
		HashMap<Resource,Integer> resourceMultisetSeller = null;
		HashMap<Resource,Integer> subjectMultisetSeller = null;
		HashMap<Resource,Integer> predicateMultisetSeller = null;
		HashMap<RDFNode,Integer> literalMultisetSeller = null;
		
		// ------------- Compute Multisets ------------------------
		logger.info("Start computing multisets.");
		
		if(input.contains("all") || input.contains("1"))
		{
			descMultisetSeller = Multisets.descMultiset(model);
		}
		if(input.contains("all") || input.contains("2"))
		{
			classifMultisetSeller = Multisets.classifMultiset(model);
		}
		if(input.contains("all") || input.contains("3"))
		{
			descmMultisetSeller = Multisets.descmMultiset(model);
		}
		if(input.contains("all") || input.contains("4"))
		{
			descmpMultisetSeller = Multisets.descmpMultiset(model);
		}
		if(input.contains("all") || input.contains("5"))
		{
			econnMultisetSeller = Multisets.econnMultiset(model);
		}
		if(input.contains("all") || input.contains("6"))
		{
			resourceMultisetSeller = Multisets.resourceMultiset(model);
		}
		if(input.contains("all") || input.contains("7"))
		{
			subjectMultisetSeller = Multisets.subjectMultiset(model);
		}
		if(input.contains("all") || input.contains("8"))
		{
			predicateMultisetSeller = Multisets.predicateMultiset(model);
		}
		if(input.contains("all") || input.contains("9"))
		{
			literalMultisetSeller = Multisets.literalMultiset(model);
		}
		
		logger.info("Done calculating Seller multisets.");
		// ----------- Compute signatures and send CBF -----------
		if(input.contains("all") || input.contains("1"))
		{
			entropySellerCommunication(descMultisetSeller);
		}
		if(input.contains("all") || input.contains("2"))
		{
			entropySellerCommunication(classifMultisetSeller);
		}
		if(input.contains("all") || input.contains("3"))
		{
			entropySellerCommunication(descmMultisetSeller);
		}
		if(input.contains("all") || input.contains("4"))
		{
			entropySellerCommunication(descmpMultisetSeller);
		}
		if(input.contains("all") || input.contains("5"))
		{
			entropySellerCommunication(econnMultisetSeller);
		}
		if(input.contains("all") || input.contains("6"))
		{
			entropySellerCommunication(resourceMultisetSeller);
		}
		if(input.contains("all") || input.contains("7"))
		{
			entropySellerCommunication(subjectMultisetSeller);
		}
		if(input.contains("all") || input.contains("8"))
		{
			entropySellerCommunication(predicateMultisetSeller);
		}
		if(input.contains("all") || input.contains("9"))
		{
			entropySellerCommunication(literalMultisetSeller);
		}
		
		logger.info("Done comuting entropies.");
	}
	
	/**
	 * creates the seller HashMap, and sends it to the buyer.
	 * @param multiset
	 */
	private static <T> void entropySellerCommunication(HashMap<T,Integer> multiset)
	{
		Server server = Server.getServer();
		
		HashMap<String, Integer> signedSellerMultiset = CBFSeller.multisetToCBF(multiset);
		server.sendObject(signedSellerMultiset);
	}
	
	/**
	 * Runs the data qualitiy step.
	 * First partitions the model.
	 * Then runs OT to share parts with the buyer.
	 * @param model
	 * @param keypair
	 * @return
	 */
	private static LinkedList<BigInteger> dataQualityStep(Model model, KeyPair keypair)
	{
		Logger logger = Log.getLogger();
		logger.info("Staring oblivious transfer step.");
		
		LinkedList<Model> partitioning = Partitioning.partitionModel(model);
		EncryptionStorage encryptionStorage = SellerPrepareKG.prepareKG(partitioning);
		LinkedList<BigInteger> keys = encryptionStorage.getKeys();
		
		LinkedList<BigInteger> randomMessages = SellerOT.sellerOT(keys, keypair);
		
		Server server = Server.getServer();
		server.sendObject(randomMessages);
		
		LinkedList<BigInteger> vValues = server.<LinkedList<BigInteger>>readObject();
		int numberOfSecrets = GetUserInput.askObliviousTransferK();
		LinkedList<LinkedList<BigInteger>> kmprime = SellerOT.sellerOTPart2(keys, numberOfSecrets, vValues, randomMessages);
		
		server.sendObject(kmprime);
		
		server.sendObject(encryptionStorage.getByteIV());
		
		server.sendObject(encryptionStorage.getEncryptedParts());
		
		// Wait for Buyer to finish as well
		logger.info(server.<String>readObject());
		
		logger.info("Number of Bytes sent after Oblivious Transfer: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received after Oblivious Transfer: " + server.getInputStreamCount());
		
		return keys;
	}
	
	public static StatisticsResults getStatisticsResults()
	{
		return statisticsResults;
	}
	
	public static void setStatisticsResults(StatisticsResults results)
	{
		statisticsResults = results;
	}
}
