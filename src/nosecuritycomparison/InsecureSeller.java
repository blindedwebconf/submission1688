package nosecuritycomparison;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.google.common.hash.BloomFilter;

import communication.Server;
import knowledgegraphpartitioning.Partitioning;
import protocol.Log;
import protocol.Tuple;
import statistics.Multisets;
import statistics.Statistics;
import statistics.StatisticsResults;

public class InsecureSeller 
{
	public static void runAsSellerNoPrivacy(Model model, Scanner scanner)
	{
		Logger logger = Log.getLogger();
		
		logger.info("Enter path to your Keystore.");
		String pathToKeyStore = scanner.next();
		
		logger.info("Enter Keystore password.");
		String keyStorePw = scanner.next();
		
		System.setProperty("javax.net.ssl.keyStore", pathToKeyStore); 
		System.setProperty("javax.net.ssl.keyStorePassword", keyStorePw);
		
		// Communication test
		Server server = Server.getServer();
		
		// ------------- Intersection ------------------
		logger.info("Start Intersection step.");
		BloomFilter<String> bf = InsecureIntersection.trainBloomFilter(model);
		server.sendObject(bf);
		
		// Wait for Buyer to finish as well
		logger.info(server.<String>readObject());
		
		logger.info("Number of Bytes sent after Intersection: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received after Intersection: " + server.getInputStreamCount());
		
		// ------------- Statistics ------------------
		logger.info("Start Statistics step.");
		StatisticsResults statisticsResults = Statistics.statistics(model);
		server.sendObject(statisticsResults);
		
		// Wait for Buyer to finish as well
		logger.info(server.<String>readObject());
		
		logger.info("Number of Bytes sent after Statistics: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received after Statistics: " + server.getInputStreamCount());
		
		// ------------- Entropy ------------------
		logger.info("Start Entropy step.");
		logger.info("Start computing multisets.");
		HashMap<Tuple<Resource, RDFNode>,Integer> descMultisetSeller = Multisets.descMultiset(model);
		HashMap<Tuple<Resource, RDFNode>,Integer> classifMultisetSeller = Multisets.classifMultiset(model);
		HashMap<Tuple<Resource,RDFNode>,Integer> descmMultisetSeller = Multisets.descmMultiset(model);
		HashMap<Resource,Integer> descmpMultisetSeller = Multisets.descmpMultiset(model);
		HashMap<Resource,Integer> econnMultisetSeller = Multisets.econnMultiset(model);
		HashMap<Resource,Integer> resourceMultisetSeller = Multisets.resourceMultiset(model);
		HashMap<Resource,Integer> subjectMultisetSeller = Multisets.subjectMultiset(model);
		HashMap<Resource,Integer> predicateMultisetSeller = Multisets.predicateMultiset(model);
		HashMap<RDFNode,Integer> literalMultisetSeller = Multisets.literalMultiset(model);
		
		logger.info("Done computing multisets. Start sending multisets to Buyer");
		
		server.sendHashMap(descMultisetSeller);
		server.sendHashMap(classifMultisetSeller);
		server.sendHashMap(descmMultisetSeller);
		server.sendHashMap3(descmpMultisetSeller);
		server.sendHashMap3(econnMultisetSeller);
		server.sendHashMap3(resourceMultisetSeller);
		server.sendHashMap3(subjectMultisetSeller);
		server.sendHashMap3(predicateMultisetSeller);
		server.sendHashMap2(literalMultisetSeller);
		
		// Wait for Buyer to finish as well
		logger.info(server.<String>readObject());
		
		logger.info("Number of Bytes sent after Entropies: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received after Entropies: " + server.getInputStreamCount());
		
		// ------------- Share Model Part ------------------
		logger.info("Start Oblivious Transfer step.");

		LinkedList<Model> partitions = Partitioning.partitionDBSCAN(model);
//		LinkedList<Model> partitions = Partitioning.dbscan(model, 10);
//		LinkedList<Model> partitions = Partitioning.partitionNeighborhood(model, 5);
//		LinkedList<Model> partitions = Partitioning.partitionResorces(model);
		server.sendModel(partitions.getFirst());
		
		logger.info("Number of Bytes sent after OT: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received after OT: " + server.getInputStreamCount());
		
		// -------------- Send Seller model ----------------
		server.sendModel(model);
		
		// Wait for Buyer to finish as well
		logger.info(server.<String>readObject());
		
		logger.info("Number of Bytes sent: " + server.getOutputStreamCount());
		logger.info("Number of Bytes received: " + server.getInputStreamCount());
		
		scanner.close();
	}
}
