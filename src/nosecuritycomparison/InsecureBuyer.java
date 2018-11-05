package nosecuritycomparison;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.google.common.hash.BloomFilter;

import communication.Client;
import protocol.Log;
import protocol.Tuple;
import statistics.CBFBuyer;
import statistics.EntropiesEnum;
import statistics.Entropy;
import statistics.Multisets;
import statistics.Statistics;
import statistics.StatisticsResults;

public class InsecureBuyer 
{
	public static void runAsBuyerNoPrivacy(Model model, Scanner scanner)
	{
		Logger logger = Log.getLogger();
		
		logger.info("Enter path to your Keystore.");
		String pathToTrustStore = scanner.next();
		
		logger.info("Enter Keystore password.");
		String trustStorePw = scanner.next();
		
		System.setProperty("javax.net.ssl.trustStore", pathToTrustStore); 
		System.setProperty("javax.net.ssl.trustStorePassword", trustStorePw);
		
		// Communication test
		Client client = Client.getClient();
		
		// ------------- Intersection ------------------
		logger.info("Start Intersection step.");
		BloomFilter<String> bf = client.<BloomFilter<String>>readObject();
		
		Model intersection = InsecureIntersection.findIntersection(model, bf);
		
		client.sendObject("Buyer done with calculating the Intersection.");
		logger.info("Done with Intersection step.");
		
		logger.info("Number of Bytes sent after Intersection: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received after Intersection: " + client.getInputStreamCount());
		
		// ------------- Statistics ------------------
		logger.info("Starting Statistics step.");
		StatisticsResults sellerStatisticsResults = client.<StatisticsResults>readObject();
		StatisticsResults buyerStatisticsResults = Statistics.statistics(model);
		logger.info("Seller statistics results: ");
		Statistics.outputStatistics(sellerStatisticsResults);
		logger.info("Buyer statistics results: ");
		Statistics.outputStatistics(buyerStatisticsResults);
		
		client.sendObject("Buyer done with calculating the Statistics.");
		logger.info("Done with Statistics step.");
		
		logger.info("Number of Bytes sent after Statistics: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received after Statistics: " + client.getInputStreamCount());
		
		// ------------- Entropy ------------------
		logger.info("Start Entropy step.");

		logger.info("Start computing multisets.");
		HashMap<Tuple<Resource, RDFNode>,Integer> descMultisetBuyer = Multisets.descMultiset(model);
		HashMap<Tuple<Resource, RDFNode>,Integer> descMultisetIntersection = Multisets.descMultiset(intersection);
		HashMap<Tuple<Resource, RDFNode>,Integer> classifMultisetBuyer = Multisets.classifMultiset(model);
		HashMap<Tuple<Resource, RDFNode>,Integer> classifMultisetIntersection = Multisets.classifMultiset(intersection);
		HashMap<Tuple<Resource, RDFNode>,Integer> descmMultisetBuyer = Multisets.descmMultiset(model);
		HashMap<Tuple<Resource, RDFNode>,Integer> descmMultisetIntersection = Multisets.descmMultiset(intersection);
		HashMap<Resource,Integer> descmpMultisetBuyer = Multisets.descmpMultiset(model);
		HashMap<Resource,Integer> descmpMultisetIntersection = Multisets.descmpMultiset(intersection);
		HashMap<Resource,Integer> econnMultisetBuyer = Multisets.econnMultiset(model);
		HashMap<Resource,Integer> econnMultisetIntersection = Multisets.econnMultiset(intersection);
		HashMap<Resource,Integer> resourceMultisetBuyer = Multisets.resourceMultiset(model);
		HashMap<Resource,Integer> resourceMultisetIntersection = Multisets.resourceMultiset(intersection);
		HashMap<Resource,Integer> subjectMultisetBuyer = Multisets.subjectMultiset(model);
		HashMap<Resource,Integer> subjectMultisetIntersection = Multisets.subjectMultiset(intersection);
		HashMap<Resource,Integer> predicateMultisetBuyer = Multisets.predicateMultiset(model);
		HashMap<Resource,Integer> predicateMultisetIntersection = Multisets.predicateMultiset(intersection);
		HashMap<RDFNode,Integer> literalMultisetBuyer = Multisets.literalMultiset(model);
		HashMap<RDFNode,Integer> literalMultisetIntersection = Multisets.literalMultiset(intersection);
		
		logger.info("Done computing multisets. Start computing Entropies.");
		
		HashMap<Tuple<Resource, RDFNode>, Integer> descMultisetSeller = client.readHashMap();
		insecureMultisetsToEntropy(descMultisetBuyer, descMultisetIntersection, descMultisetSeller, EntropiesEnum.DESC);
		
		HashMap<Tuple<Resource, RDFNode>, Integer> classifMultisetSeller = client.readHashMap();
		insecureMultisetsToEntropy(classifMultisetBuyer, classifMultisetIntersection, classifMultisetSeller, EntropiesEnum.CLASSIF);
		
		HashMap<Tuple<Resource, RDFNode>, Integer> descmMultisetSeller = client.readHashMap();
		insecureMultisetsToEntropy(descmMultisetBuyer, descmMultisetIntersection, descmMultisetSeller, EntropiesEnum.DESCM);
		
		HashMap<Resource,Integer> descmpMultisetSeller = client.readHashMap3();
		insecureMultisetsToEntropy(descmpMultisetBuyer, descmpMultisetIntersection, descmpMultisetSeller, EntropiesEnum.DESCMP);
		
		HashMap<Resource,Integer> econnMultisetSeller = client.readHashMap3();
		insecureMultisetsToEntropy(econnMultisetBuyer, econnMultisetIntersection, econnMultisetSeller, EntropiesEnum.ECONN);
		
		HashMap<Resource,Integer> resourceMultisetSeller = client.readHashMap3();
		insecureMultisetsToEntropy(resourceMultisetBuyer, resourceMultisetIntersection, resourceMultisetSeller, EntropiesEnum.RESOURCE);
		
		HashMap<Resource,Integer> subjectMultisetSeller = client.readHashMap3();
		insecureMultisetsToEntropy(subjectMultisetBuyer, subjectMultisetIntersection, subjectMultisetSeller, EntropiesEnum.SUBJECT);
		
		HashMap<Resource,Integer> predicateMultisetSeller = client.readHashMap3();
		insecureMultisetsToEntropy(predicateMultisetBuyer, predicateMultisetIntersection, predicateMultisetSeller, EntropiesEnum.PREDICATE);
		
		HashMap<RDFNode,Integer> literalMultisetSeller = client.readHashMap2();
		insecureMultisetsToEntropy(literalMultisetBuyer, literalMultisetIntersection, literalMultisetSeller, EntropiesEnum.LITERAL);
		
		client.sendObject("Buyer done with calculating the Entropies.");
		
		logger.info("Number of Bytes sent after Entropies: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received after Entropies: " + client.getInputStreamCount());
		
		// ------------- Share Model Part ------------------
		logger.info("Start sharing graph part step.");
		
		Model sharedModelPart = client.readModel();
		
		logger.info("Received model part with " + sharedModelPart.size() + " statements.");
		
		logger.info("Number of Bytes sent after OT: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received after OT: " + client.getInputStreamCount());
		
		// -------------- Receive the Sellers model --------
		Model sellerModel = client.readModel();
		
		client.sendObject("All Steps completed.");
		logger.info("All steps completed.");
		
		logger.info("Number of Bytes sent: " + client.getOutputStreamCount());
		logger.info("Number of Bytes received: " + client.getInputStreamCount());
		
		scanner.close();
	}
	
	private static <K> void insecureMultisetsToEntropy(HashMap<K, Integer> multisetBuyer, HashMap<K, Integer> multisetIntersection, HashMap<K, Integer> multisetSeller,  EntropiesEnum name)
	{
		Logger logger = Log.getLogger();
		
		LinkedList<Integer> unionCount = InsecureEntropy.insecureUnifiedCount(multisetBuyer, multisetIntersection, multisetSeller);
		double entropy = Entropy.calculateEntropy(unionCount);
		logger.info(name.toString() + " entropy: " + entropy);
		
		LinkedList<Integer> buyerCounts = new LinkedList<Integer>(multisetBuyer.values());
		double entropyBuyer = Entropy.calculateEntropy(buyerCounts);
		logger.info(name.toString() + " buyer entropy: " + entropyBuyer);
		
		double entropyGain = entropy - entropyBuyer;
		logger.info(name.toString() + " entropy gain: " + entropyGain);
		
		LinkedList<Integer> sellerCounts = new LinkedList<Integer>(multisetSeller.values());
		double entropySeller = Entropy.calculateEntropy(sellerCounts);
		logger.info(name.toString() + " seller entropy: " + entropySeller);
	}
}
