package protocol;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import com.google.common.hash.BloomFilter;

import buyertests.BuyerTests;
import communication.Client;
import dataquality.BuyerObtainKGPart;
import oblivioustransfer.BuyerOT;
import privatesetintersection.BuyerBlindSignatures;
import privatesetintersection.KGIntersectionBuyer;
import privatesetintersection.SellerBlindSignatures;
import statistics.CBFBuyer;
import statistics.EntropiesEnum;
import statistics.Entropy;
import statistics.EntropyResults;
import statistics.EntropyStore;
import statistics.Multisets;
import statistics.Statistics;
import statistics.StatisticsResults;

public class Buyer 
{
	private static String modelFolder = "";
	
	private static HashMap<Tuple<Resource, RDFNode>,Integer> descMultisetBuyer = null;
	private static HashMap<Tuple<Resource, RDFNode>,Integer> classifMultisetBuyer = null;
	private static HashMap<Tuple<Resource, RDFNode>,Integer> descmMultisetBuyer = null;
	private static HashMap<Resource,Integer> descmpMultisetBuyer = null;
	private static HashMap<Resource,Integer> econnMultisetBuyer = null;
	private static HashMap<Resource,Integer> resourceMultisetBuyer = null;
	private static HashMap<Resource,Integer> subjectMultisetBuyer = null;
	private static HashMap<Resource,Integer> predicateMultisetBuyer = null;
	private static HashMap<RDFNode,Integer> literalMultisetBuyer = null;
	
	private static LinkedList<Tuple<String, Tuple<Resource,RDFNode>>> descSignaturesWithElements = null;
	private static LinkedList<Tuple<String, Tuple<Resource,RDFNode>>> classifSignaturesWithElements = null;
	private static LinkedList<Tuple<String, Tuple<Resource,RDFNode>>> descmSignaturesWithElements = null;
	private static LinkedList<Tuple<String, Resource>> descmpSignaturesWithElements = null;
	private static LinkedList<Tuple<String, Resource>> econnSignaturesWithElements = null;
	private static LinkedList<Tuple<String, Resource>> resourceSignaturesWithElements = null;
	private static LinkedList<Tuple<String, Resource>> subjectSignaturesWithElements = null;
	private static LinkedList<Tuple<String, Resource>> predicateSignaturesWithElements = null;
	private static LinkedList<Tuple<String, RDFNode>> literalSignaturesWithElements = null;
	
	private static BloomFilter<String> bf = null;
	

	/**
	 * This method gets called if the instance of this protocol is run as buyer.
	 * It calls all the steps that the buyer needs to do.
	 * 
	 * @param model Model of the buyers knowledge graph
	 */
	public static void runAsBuyer(Model model, Scanner scanner)
	{
		Logger logger = Log.getLogger();
		
		logger.info("Enter path to your Truststore.");
		String pathToTrustStore = scanner.nextLine();
		
		logger.info("Enter Truststore password.");
		String trustStorePw = scanner.nextLine();
		
		System.setProperty("javax.net.ssl.trustStore", pathToTrustStore);
	    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePw);
		
		Client client = Client.getClient();
		
		RSAPublicKey key = client.<RSAPublicKey>readObject();
		BuyerBlindSignatures.setPublicKey(key);
		
		// ----------- Get entropy signatures ---------------
		boolean runEntropy = GetUserInput.runStep("Calculate Entropies. [yes, no]");
		if(runEntropy)
		{
			entropyBuyerSignatures(model);
		}
		
		// ----------- Intersection -------------------------
		boolean runIntersection = GetUserInput.runStep("Calculate the intersection. [yes, no]");
		Model intersection = ModelFactory.createDefaultModel();
		if(runIntersection)
		{
			intersection = intersectionStep(model);
		}
		GetUserInput.continueProtocol();
		
		// ----------- Entropy and Statistics -------------------------
		EntropyResults entropyResults = null;
		if(runEntropy)
		{
			entropyResults = entropyBuyer(model, intersection);
			
			logger.info("Number of Bytes sent after Entropies: " + client.getOutputStreamCount());
			logger.info("Number of Bytes received after Entropies: " + client.getInputStreamCount());
		}
		GetUserInput.continueProtocol();
		
		boolean runStatistics = GetUserInput.runStep("Calculate descriptive Statistics. [yes, no]");
		StatisticsResults sellerStatisticsResults = null;
		if(runStatistics)
		{
			sellerStatisticsResults = statisticsStep(model);
			// So they are set if the verification step tests the data quality step, and the balancedDBSCAN gets his minStatenents
			Seller.setStatisticsResults(sellerStatisticsResults);
		}
		GetUserInput.continueProtocol();
		
		// ----------- Oblivious Transfer -------------------------
		boolean runObliviousTransfer = GetUserInput.runStep("Run Oblivious Transfer. [yes, no]");
		LinkedList<byte[]> encryptedParts = new LinkedList<byte[]>();
		if(runObliviousTransfer)
		{
			encryptedParts = dataQualitiyStep(model, key);
		}
		GetUserInput.continueProtocol();
		
		// -------- Receive model and data from Seller ------------
		logger.info("Start receiving model and used keys from Seller.");
		Model sellerModel = client.readModel();
		
		KeyPair sellerIntersectionEntropyKeyPair =  client.<KeyPair>readObject();
		SellerBlindSignatures.buyerTestSetKey(sellerIntersectionEntropyKeyPair);
		
		LinkedList<BigInteger> otKeysBI = null;
		if(runObliviousTransfer)
		{
			otKeysBI = client.<LinkedList<BigInteger>>readObject();
		}
		
		client.sendObject("Buyer received Sellers model, intersection keypair, and OT keys.");
		logger.info("Done receiving model and used keys from Seller.");
		
		logger.info("Seller model contains " + sellerModel.size() + " Statements.");
		
		// -------- Check if the Seller Cheated -------------------
		logger.info("Start verification step.");
		boolean fair = true;
		if(runIntersection)
		{
			fair = fair & BuyerTests.testIntersection(model, sellerModel, intersection, bf);
		}
		if(runStatistics)
		{
			fair = fair & BuyerTests.testStatistics(sellerModel, sellerStatisticsResults);
		}
		if(runEntropy)
		{
			fair = fair & BuyerTests.testEntropies(model, sellerModel, entropyResults);
		}
		if(runObliviousTransfer)
		{
			fair = fair & BuyerTests.testOblivousTransfer(sellerModel, encryptedParts, otKeysBI, BuyerObtainKGPart.getIV());
		}
		logger.info("Done verification step.");
		logger.info("Seller behaved fairly: " + fair);
		
		logger.info("Number of Bytes sent: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received: " + client.getInputStreamCount());
		
		scanner.close();
	}
	
	/**
	 * Computes the intersection between the sellers and the buyers model
	 * @param model of the buyer
	 * @return intersection
	 */
	private static Model intersectionStep(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start Intersection step.");
		
		HashMap<Long, Statement> numberStatementMap = KGIntersectionBuyer.numberStatements(model);
		
		LinkedList<Tuple<String,Long>> signatures = BuyerBlindSignatures.bobBlindSignature(numberStatementMap);
		
		Client client = Client.getClient();
		bf = client.<BloomFilter<String>>readObject();
		
		Model intersection = KGIntersectionBuyer.determineIntersection(bf, signatures, numberStatementMap);
		
		client.sendObject("Buyer done with calculating the intersection.");
		logger.info("Done with Intersection step.");
		
		if(modelFolder.equals(""))
		{
			modelFolder = GetUserInput.askFolder();
		}
		ModelTools.writeModelToFile(intersection, modelFolder.concat("Intersection.ttl"));
		logger.info("Parts have been written to folder.");
		
		logger.info("Number of Bytes sent after Intersection: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received after Intersection: " + client.getInputStreamCount());
		
		return intersection;
	}
	
	/**
	 * Gets signatures for the buyers multisets needed for entropy calculation
	 * @param model
	 */
	private static void entropyBuyerSignatures(Model model)
	{
		LinkedList<String> input = GetUserInput.entropiesToCompute();
		if(input.contains("all") || input.contains("1"))
		{
			descMultisetBuyer = Multisets.descMultiset(model);
			descSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(descMultisetBuyer);
		}
		if(input.contains("all") || input.contains("2"))
		{
			classifMultisetBuyer = Multisets.classifMultiset(model);
			classifSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(classifMultisetBuyer);
		}
		if(input.contains("all") || input.contains("3"))
		{
			descmMultisetBuyer = Multisets.descmMultiset(model);
			descmSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(descmMultisetBuyer);
		}
		if(input.contains("all") || input.contains("4"))
		{
			descmpMultisetBuyer = Multisets.descmpMultiset(model);
			descmpSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(descmpMultisetBuyer);
		}
		if(input.contains("all") || input.contains("5"))
		{
			econnMultisetBuyer = Multisets.econnMultiset(model);
			econnSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(econnMultisetBuyer);
		}
		if(input.contains("all") || input.contains("6"))
		{
			resourceMultisetBuyer = Multisets.resourceMultiset(model);
			resourceSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(resourceMultisetBuyer);
		}
		if(input.contains("all") || input.contains("7"))
		{
			subjectMultisetBuyer = Multisets.subjectMultiset(model);
			subjectSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(subjectMultisetBuyer);
		}
		if(input.contains("all") || input.contains("8"))
		{
			predicateMultisetBuyer = Multisets.predicateMultiset(model);
			predicateSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(predicateMultisetBuyer);
		}
		if(input.contains("all") || input.contains("9"))
		{
			literalMultisetBuyer = Multisets.literalMultiset(model);
			literalSignaturesWithElements = CBFBuyer.getBuyerElementsSignatures(literalMultisetBuyer);
		}
	}
	
	/**
	 * Computes various entropies on the buyers, the sellers, and the unified model.
	 * @param buyer model
	 * @param intersection between the buyer and seller model
	 * @return Class containing results of entropy calculation
	 */
	private static EntropyResults entropyBuyer(Model buyer, Model intersection)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing entropies.");
		
		EntropyResults results = new EntropyResults();
		
		HashMap<Tuple<Resource, RDFNode>,Integer> descMultisetIntersection = null;
		HashMap<Tuple<Resource, RDFNode>,Integer> classifMultisetIntersection = null;
		HashMap<Tuple<Resource, RDFNode>,Integer> descmMultisetIntersection = null;
		HashMap<Resource,Integer> descmpMultisetIntersection = null;
		HashMap<Resource,Integer> econnMultisetIntersection = null;
		HashMap<Resource,Integer> resourceMultisetIntersection = null;
		HashMap<Resource,Integer> subjectMultisetIntersection = null;
		HashMap<Resource,Integer> predicateMultisetIntersection = null;
		HashMap<RDFNode,Integer> literalMultisetIntersection = null;
		
		logger.info("Start computing multisets.");
		
		if(!(descMultisetBuyer == null))
		{
			descMultisetIntersection = Multisets.descMultiset(intersection);
		}
		if(!(classifMultisetBuyer == null))
		{
			classifMultisetIntersection = Multisets.classifMultiset(intersection);
		}
		if(!(descmMultisetBuyer == null))
		{
			descmMultisetIntersection = Multisets.descmMultiset(intersection);
		}
		if(!(descmpMultisetBuyer == null))
		{
			descmpMultisetIntersection = Multisets.descmpMultiset(intersection);
		}
		if(!(econnMultisetBuyer == null))
		{
			econnMultisetIntersection = Multisets.econnMultiset(intersection);
		}
		if(!(resourceMultisetBuyer == null))
		{
			resourceMultisetIntersection = Multisets.resourceMultiset(intersection);
		}
		if(!(subjectMultisetBuyer == null))
		{
			subjectMultisetIntersection = Multisets.subjectMultiset(intersection);
		}
		if(!(predicateMultisetBuyer == null))
		{
			predicateMultisetIntersection = Multisets.predicateMultiset(intersection);
		}
		if(!(literalMultisetBuyer == null))
		{
			literalMultisetIntersection = Multisets.literalMultiset(intersection);
		}
		
		logger.info("Done computing multisets.");
		
		if(!(descMultisetBuyer == null))
		{
			multisetsToEntropy(descMultisetBuyer, descMultisetIntersection, descSignaturesWithElements, EntropiesEnum.DESC, results);
		}
		
		if(!(classifMultisetBuyer == null))
		{
			multisetsToEntropy(classifMultisetBuyer, classifMultisetIntersection, classifSignaturesWithElements, EntropiesEnum.CLASSIF, results);
		}
		
		if(!(descmMultisetBuyer == null))
		{
			multisetsToEntropy(descmMultisetBuyer, descmMultisetIntersection, descmSignaturesWithElements, EntropiesEnum.DESCM, results);
		}
		
		if(!(descmpMultisetBuyer == null))
		{
			multisetsToEntropy(descmpMultisetBuyer, descmpMultisetIntersection, descmpSignaturesWithElements, EntropiesEnum.DESCMP, results);
		}
		
		if(!(econnMultisetBuyer == null))
		{
			multisetsToEntropy(econnMultisetBuyer, econnMultisetIntersection, econnSignaturesWithElements, EntropiesEnum.ECONN, results);
		}
		
		if(!(resourceMultisetBuyer == null))
		{
			multisetsToEntropy(resourceMultisetBuyer, resourceMultisetIntersection, resourceSignaturesWithElements, EntropiesEnum.RESOURCE, results);
		}
		
		if(!(subjectMultisetBuyer == null))
		{
			multisetsToEntropy(subjectMultisetBuyer, subjectMultisetIntersection, subjectSignaturesWithElements, EntropiesEnum.SUBJECT, results);
		}
		
		if(!(predicateMultisetBuyer == null))
		{
			multisetsToEntropy(predicateMultisetBuyer, predicateMultisetIntersection, predicateSignaturesWithElements, EntropiesEnum.PREDICATE, results);
		}
		
		if(!(literalMultisetBuyer == null))
		{
			multisetsToEntropy(literalMultisetBuyer, literalMultisetIntersection, literalSignaturesWithElements, EntropiesEnum.LITERAL, results);
		}
		
		logger.info("Done computing entropies.");
		outputEntropies(results);
		return results;
	}
	
	/**
	 * Computes entropies from multisets (HashMaps)
	 * @param multisetBuyer
	 * @param multisetIntersection
	 * @param name of entropy to compute
	 * @param results class to store results in
	 */
	private static <K> void multisetsToEntropy(HashMap<K, Integer> multisetBuyer, HashMap<K, Integer> multisetIntersection, LinkedList<Tuple<String, K>> signaturesWithElements, EntropiesEnum name, EntropyResults results)
	{
		Logger logger = Log.getLogger();
		
		// Entropy of buyer model only
		LinkedList<Integer> buyerCounts = new LinkedList<Integer>(multisetBuyer.values());
		double entropyBuyer = Entropy.calculateEntropy(buyerCounts);
		
		// Get seller multiset and store it
		Client client = Client.getClient();
		HashMap<String, Integer> signedSellerMultiset = client.<HashMap<String, Integer>>readObject();
		EntropyStore entropyStore = results.newEntropy(name);
		entropyStore.setSellerMultiset(signedSellerMultiset);
		logger.info("Got multiset");
		
		// Entropy for buyer and seller model combined
		LinkedList<Integer> countsIntersection = CBFBuyer.multisetIntersection(multisetBuyer, multisetIntersection, signedSellerMultiset, signaturesWithElements);
		double entropy = Entropy.calculateEntropy(countsIntersection);
		
		// Entropy gain/difference
		double entropyGain = entropy - entropyBuyer;
		entropyStore.setEntropy(entropy);
		entropyStore.setEntropyBuyer(entropyBuyer);
		entropyStore.setEntropyGain(entropyGain);
		
		// Entropy of seller model only
		LinkedList<Integer> sellerCounts = new LinkedList<Integer>(signedSellerMultiset.values());
		double entropySeller = Entropy.calculateEntropy(sellerCounts);
		entropyStore.setEntropySeller(entropySeller);
	}
	
	/**
	 * Outputs the results of the entropy calculations
	 * @param results
	 */
	private static void outputEntropies(EntropyResults results)
	{
		Logger logger = Log.getLogger();
		
		for(EntropiesEnum name : EntropiesEnum.values())
		{
			EntropyStore store = results.getEntropyStore(name);
			if(store != null)
			{
				logger.info(name + " results: \n" 
							+ "Entropy combined models: " + store.getEntropy() + "\n"
							+ "Entropy buyer model: " + store.getEntropyBuyer() + "\n"
							+ "Entropy gain: " + store.getEntropyGain() + "\n"
							+ "Entropy seller model: " + store.getEntropySeller());
			}
		}
	}
	
	/**
	 * Runs the statistics step
	 * @param model
	 * @return statistics results of the seller
	 */
	private static StatisticsResults statisticsStep(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start Statistics step.");
		
		Client client = Client.getClient();
		StatisticsResults sellerStatisticsResults = client.<StatisticsResults>readObject();
		logger.info("Statistics of Sellers model:");
		Statistics.outputStatistics(sellerStatisticsResults);
		
		StatisticsResults buyerStatisticsResults = Statistics.statistics(model);
		logger.info("Statistics of Buyers model:");
		Statistics.outputStatistics(buyerStatisticsResults);

		client.sendObject("Buyer done with calculating the Entropies and Statistics.");
		logger.info("Done with Statistics step.");
		
		logger.info("Number of Bytes sent after Statistics: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received after Statistics: " + client.getInputStreamCount());
		
		return sellerStatisticsResults;
	}
	
	/**
	 * Runs the data quality step.
	 * Obtains graph parts from the Seller and writes them to files.
	 * @param model
	 * @param key RSAKey publickey of the seller
	 * @return the encrypted parts of the sellers model
	 */
	private static LinkedList<byte[]> dataQualitiyStep(Model model, RSAPublicKey key)
	{
		Logger logger = Log.getLogger();
		logger.info("Start oblivious transfer step.");
		
		int k = GetUserInput.askObliviousTransferK();
		
		Client client = Client.getClient();
		LinkedList<BigInteger> randomMessages = client.<LinkedList<BigInteger>>readObject();
		LinkedList<BigInteger> vValues = BuyerOT.buyerOT(k, key, randomMessages);
		
		client.sendObject(vValues);
		
		LinkedList<LinkedList<BigInteger>> kmprime = client.<LinkedList<LinkedList<BigInteger>>>readObject();
		LinkedList<BigInteger> keysBI = BuyerOT.buyerOTPart2(key, kmprime);
		
		byte[] byteIV = client.<byte[]>readObject();
		LinkedList<byte[]> encryptedParts = client.<LinkedList<byte[]>>readObject();
		LinkedList<Model> obtained = BuyerObtainKGPart.obtainKGPart(keysBI, byteIV, encryptedParts);
		logger.info("Number of KG parts received: " + obtained.size());
		
		if(modelFolder.equals(""))
		{
			modelFolder = GetUserInput.askFolder();
		}
		ModelTools.writeModelsToFile(obtained, modelFolder);
		logger.info("Parts have been written to folder.");
		
		client.sendObject("Buyer done with Oblivious Transfer/Data Quality Check.");
		logger.info("Done with oblivious transfer step.");
		
		logger.info("Number of Bytes sent after Oblivious Transfer: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received after Oblivious Transfer: " + client.getInputStreamCount());
		
		return encryptedParts;
	}
	
}
