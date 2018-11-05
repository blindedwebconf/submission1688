package buyertests;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.logging.Logger;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import com.google.common.hash.BloomFilter;

import dataquality.BuyerObtainKGPart;
import dataquality.SellerPrepareKG;
import knowledgegraphpartitioning.Partitioning;
import privatesetintersection.KGIntersectionSeller;
import privatesetintersection.SellerBlindSignatures;
import protocol.GetUserInput;
import protocol.Log;
import protocol.ModelTools;
import protocol.Tuple;
import statistics.CBFSeller;
import statistics.EntropiesEnum;
import statistics.Entropy;
import statistics.EntropyResults;
import statistics.Multisets;
import statistics.Statistics;
import statistics.StatisticsResults;

public class BuyerTests {
	
	/**
	 *  Checks if the intersection is correct
	 *  Compares the intersection calculated in the protocol with the true intersection
	 *  If the protocol intersection contains all elements of the true intersection and more this can be the result of the possible false positives in the used Bloom Filter
	 *  In that case it calculates whether the extra elements in the intersection are a reasonable number and close to the expeced false positives.
	 *  In this case it can also test if the Bloom filter was created and trained correctly by the seller
	 *  
	 * @param buyerKG model used by the buyer instance of this protocol
	 * @param sellerKG model used by the seller instance of this protocol
	 * @param intersection claimed intersection between buyerKG and sellerKG
	 * @param bf be used to compute intersection
	 * @return returns true if the given intersection is the same as the intersection between the two models
	 */
	public static boolean testIntersection(Model buyerKG, Model sellerKG, Model intersection, BloomFilter<String> bf)
	{
		Logger logger = Log.getLogger();
		logger.info("Start testing intersection.");
		
		// Calculate the intersection without using a BF on both models in plain text
		Model trueIntersection = buyerKG.intersection(sellerKG);
		
		// Check if true intersection matches the one resulting from the protocol
		boolean correct = intersection.isIsomorphicWith(trueIntersection);
		
		logger.info("The true intersection matches the intersection resulting from the protocol: " + correct);
		
		// Check if the protocol intersection contains strictly more elements then the true intersection (suspecting false positives from the BF)
		if(!correct)
		{
			if(intersection.containsAll(trueIntersection))
			{
				double extraElements = intersection.size() - trueIntersection.size();
				double extraPercentage = extraElements / buyerKG.size() * 100;
				logger.info("The protocol intersection contains all elements of the true intersection.");
				logger.info("Protocol intersection contains " + extraElements + " more elements then the true intersection.");
				logger.info("This equals " + extraPercentage + "% more elements.");
				logger.info("Using a Bloom Filter for the intersection can lead to false positives. Expected false positive rate: " + bf.expectedFpp());
			}  else
			{
				Model missingStatementsModel = trueIntersection.difference(intersection);
				logger.info("There are " + missingStatementsModel.size() + " statements missing in the protocol intersection which are in the true intersection. \n"
							+ "This cannot be explained by the Bloom filter as false negatives are not possible. \n"
							+ "This is not supposed to happen if both parties follow the protocol");
			}
		}
		
		if(!correct)
		{
			logger.info("Testing if the Bloomfilter was computed correctly.");
			
			LinkedList<String> signatures = SellerBlindSignatures.aliceBlindSignatures(sellerKG);
			double fpp = GetUserInput.getBloomFilterFPP();
			BloomFilter<String> testBF = KGIntersectionSeller.trainBloomFilter(fpp, signatures);
			
			BitSet testBFBits = getBitSet(testBF);
			BitSet bfBits = getBitSet(bf);

			boolean bfMatch = bfBits.equals(testBFBits);
			if(bfMatch)
			{
				logger.info("The Bloom filter computed in the protocol matches the test Bloom filter.");
				correct = true;
			} else {
				logger.info("The protocol Bloom filter and the test Bloom filter do not match.");
			}
		}
		
		logger.info("Done testing intersection.");
		return correct;
	}
	
	/**
	 * 
	 * @param bloomFilter
	 * @return BitSet which bits of the Bloom filter are set.
	 */
	private static <T> BitSet getBitSet(BloomFilter<T> bloomFilter) 
	{
	    try 
	    {
	        Field bitsField = BloomFilter.class.getDeclaredField("bits");
	        bitsField.setAccessible(true);
	        Object bitArray = bitsField.get(bloomFilter);
	        Field dataField = bitArray.getClass().getDeclaredField("data");
	        dataField.setAccessible(true);
//	        return BitSet.valueOf((long[]) dataField.get(bitArray));
	        AtomicLongArray bitsALArray = (AtomicLongArray) dataField.get(bitArray);
	        long[] bitLArray = new long[bitsALArray.length()];
	        for(int i = 0; i < bitsALArray.length(); i++)
	        {
	        	bitLArray[i] = bitsALArray.get(i);
	        }
	        return BitSet.valueOf(bitLArray);
	    } catch (NoSuchFieldException | IllegalAccessException e) {
	        throw new RuntimeException(e);
	    }
	}
	
	/**
	 * Recalculates the statistics on the sellers model
	 * Checks if all values match the ones the seller claims
	 * 
	 * @param sellerKG model used by the seller instance
	 * @param sellerResults statistic results the seller computed on his model
	 * @return true if all statistics are correct, false otherwise
	 */
	public static boolean testStatistics(Model sellerKG, StatisticsResults sellerResults)
	{
		Logger logger = Log.getLogger();
		logger.info("Start testing Statistics.");
		
		// Recalculate the statistics
		StatisticsResults trueResults = Statistics.statistics(sellerKG);
		
		boolean fullMatch = true;	// Stores if all seller values match the true values
		
		// For all statistics check if they match
		// Corresponding output
		// set fullMatch to false if a nonmatching statistic is found
		if(trueResults.getSize() == sellerResults.getSize())
		{
			logger.info("Claimed size of the Sellers model matches the true size.");
		} else
		{
			logger.info("Claimed size of the Sellers model does not match the true size. \n Claimed: " + sellerResults.getSize() + " True: " + trueResults.getSize());
			fullMatch = false;
		}
		
		if(trueResults.getResources() == sellerResults.getResources())
		{
			logger.info("Claimed number of resources of the Sellers model matches the true number of resources.");
		} else
		{
			logger.info("Claimed number of resources of the Sellers model does not match the true number of resources. \n Claimed: " + sellerResults.getResources() + " True: " + trueResults.getResources());
			fullMatch = false;
		}
		
		if(trueResults.getSubjects() == sellerResults.getSubjects())
		{
			logger.info("Claimed number of subjects of the Sellers model matches the true number of subjects.");
		} else
		{
			logger.info("Claimed number of subjects of the Sellers model does not match the true number of subjects. \n Claimed: " + sellerResults.getSubjects() + " True: " + trueResults.getSubjects());
			fullMatch = false;
		}
		
		if(trueResults.getPredicates() == sellerResults.getPredicates())
		{
			logger.info("Claimed number of predicates of the Sellers model matches the true number of predicates.");
		} else
		{
			logger.info("Claimed number of predicates of the Sellers model does not match the true number of predicates. \n Claimed: " + sellerResults.getSubjects() + " True: " + trueResults.getSubjects());
			fullMatch = false;
		}
		
		if(trueResults.getObjects() == sellerResults.getObjects())
		{
			logger.info("Claimed number of objects of the Sellers model matches the true number of objects.");
		} else
		{
			logger.info("Claimed number of objects of the Sellers model does not match the true number of objects. \n Claimed: " + sellerResults.getObjects() + " True: " + trueResults.getObjects());
			fullMatch = false;
		}
		
		if(trueResults.getObjectResources() == sellerResults.getObjectResources())
		{
			logger.info("Claimed number of object resources of the Sellers model matches the true number of object resources.");
		} else
		{
			logger.info("Claimed number of object resources of the Sellers model does not match the true number of object resources. \n Claimed: " + sellerResults.getObjectResources() + " True: " + trueResults.getObjectResources());
			fullMatch = false;
		}
		
		if(trueResults.getLiterals() == sellerResults.getLiterals())
		{
			logger.info("Claimed number of literals of the Sellers model matches the true number of object literals.");
		} else
		{
			logger.info("Claimed number of literals of the Sellers model does not match the true number of object literals. \n Claimed: " + sellerResults.getLiterals() + " True: " + trueResults.getLiterals());
			fullMatch = false;
		}
		
		if(trueResults.getTotalObjectResources() == sellerResults.getTotalObjectResources())
		{
			logger.info("Claimed total number of object resources of the Sellers model matches the true total number of object resources.");
		} else
		{
			logger.info("Claimed total number of object resources of the Sellers model does not match the true total number of object resources. \n Claimed: " + sellerResults.getTotalObjectResources() + " True: " + trueResults.getTotalObjectResources());
			fullMatch = false;
		}
		
		if(trueResults.getTotalLiterals() == sellerResults.getTotalLiterals())
		{
			logger.info("Claimed total number of literals of the Sellers model matches the true total number of literals.");
		} else
		{
			logger.info("Claimed total number of literals of the Sellers model does not match the true total number of literals. \n Claimed: " + sellerResults.getTotalLiterals() + " True: " + trueResults.getTotalLiterals());
			fullMatch = false;
		}
		
		if(trueResults.getAvgOutgoingLinks() == sellerResults.getAvgOutgoingLinks())
		{
			logger.info("Claimed average of outgoing links of the Sellers model matches the true average of outgoing links.");
		} else
		{
			logger.info("Claimed average of outgoing links of the Sellers model does not match the true average of outgoing links. \n Claimed: " + sellerResults.getAvgOutgoingLinks() + " True: " + trueResults.getAvgOutgoingLinks());
			fullMatch = false;
		}
		
		if(trueResults.getAvgIncomingLinks() == sellerResults.getAvgIncomingLinks())
		{
			logger.info("Claimed average of incoming links of the Sellers model matches the true average of incoming links.");
		} else
		{
			logger.info("Claimed average of incoming links of the Sellers model does not match the true average of incoming links. \n Claimed: " + sellerResults.getAvgIncomingLinks() + " True: " + trueResults.getAvgIncomingLinks());
			fullMatch = false;
		}
		
		if(trueResults.getAvgLiterals() == sellerResults.getAvgLiterals())
		{
			logger.info("Claimed average of literals per subject of the Sellers model matches the true average of literals per subject.");
		} else
		{
			logger.info("Claimed average of literals per subject of the Sellers model does not match the true average of literals per subject. \n Claimed: " + sellerResults.getAvgLiterals() + " True: " + trueResults.getAvgLiterals());
			fullMatch = false;
		}
		
		if(trueResults.getAvgObjectResources() == sellerResults.getAvgObjectResources())
		{
			logger.info("Claimed average of object resources per subject of the Sellers model matches the true average of object resources per subject.");
		} else
		{
			logger.info("Claimed average of object resources per subject of the Sellers model does not match the true average of object resources per subject. \n Claimed: " + sellerResults.getAvgObjectResources() + " True: " + trueResults.getAvgObjectResources());
			fullMatch = false;
		}
		
		// Output whether all statistics match
		if(fullMatch)
		{
			logger.info("Statistics results of the seller fully match the results computed by the buyer on the sellers model");
		} else
		{
			logger.info("Statistics results of the seller differ from the results computed by the buyer on the sellers model. \n For differences see above.");
		}
		logger.info("Done testing Statistics.");
		
		return fullMatch;
	}
	
	/**
	 * Tests if the given entropies where computed correctly
	 * Unifies both models
	 * Recomputes multisets and entropies
	 * Recomputes seller HashMaps and checks if they match the given ones
	 * 
	 * @param buyerKG model used by the buyer instance
	 * @param sellerKG model used by the seller instance
	 * @param results calculated entropy results
	 * @return true if all entropies match, or if for the not matching entropies the multisets match
	 */
	public static boolean testEntropies(Model buyerKG, Model sellerKG, EntropyResults results)
	{
		Logger logger = Log.getLogger();
		logger.info("Start testing entropies.");
		
		Model unifiedKG = buyerKG.union(sellerKG);
		
		LinkedList<EntropiesEnum> multisetsToVerify = new LinkedList<EntropiesEnum>();
		for(EntropiesEnum name : EntropiesEnum.values())
		{
			if(results.getEntropyStore(name) != null)
			{
				boolean correct;
				switch (name) 
				{
					case DESC:
						HashMap<Tuple<Resource, RDFNode>,Integer> descMultiset = Multisets.descMultiset(unifiedKG);
						correct = checkEntropyValue(descMultiset, results, EntropiesEnum.DESC);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.DESC);
						}
						break;
					case CLASSIF:
						HashMap<Tuple<Resource, RDFNode>,Integer> classifMultiset = Multisets.classifMultiset(unifiedKG);
						correct = checkEntropyValue(classifMultiset, results, EntropiesEnum.CLASSIF);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.CLASSIF);
						}
						break;
					case DESCM:
						HashMap<Tuple<Resource, RDFNode>,Integer> descmMultiset = Multisets.descmMultiset(unifiedKG);
						correct = checkEntropyValue(descmMultiset, results, EntropiesEnum.DESCM);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.DESCM);
						}
						break;
					case DESCMP:
						HashMap<Resource,Integer> descmpMultiset = Multisets.descmpMultiset(unifiedKG);
						correct = checkEntropyValue(descmpMultiset, results, EntropiesEnum.DESCMP);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.DESCMP);
						}
						break;
					case ECONN:
						HashMap<Resource,Integer> econnMultiset = Multisets.econnMultiset(unifiedKG);
						correct = checkEntropyValue(econnMultiset, results, EntropiesEnum.ECONN);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.ECONN);
						}
						break;
					case RESOURCE:
						HashMap<Resource,Integer> resourceMultiset = Multisets.resourceMultiset(unifiedKG);
						correct = checkEntropyValue(resourceMultiset, results, EntropiesEnum.RESOURCE);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.RESOURCE);
						}
						break;
					case SUBJECT:
						HashMap<Resource,Integer> subjectMultiset = Multisets.subjectMultiset(unifiedKG);
						correct = checkEntropyValue(subjectMultiset, results, EntropiesEnum.SUBJECT);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.SUBJECT);
						}
						break;
					case PREDICATE:
						HashMap<Resource,Integer> predicateMultiset = Multisets.predicateMultiset(unifiedKG);
						correct = checkEntropyValue(predicateMultiset, results, EntropiesEnum.PREDICATE);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.PREDICATE);
						}
						break;
					case LITERAL:
						HashMap<RDFNode,Integer> literalMultiset = Multisets.literalMultiset(unifiedKG);
						correct = checkEntropyValue(literalMultiset, results, EntropiesEnum.LITERAL);
						if(!correct)
						{
							multisetsToVerify.add(EntropiesEnum.LITERAL);
						}
						break;
					default:
						logger.info("Name does not exist. Missing case.");
				}
			}
		}
		
		logger.info("Done testing entropy values, start testing Seller HashMaps.");
		
		boolean allcorrect = true;
		for(EntropiesEnum name : multisetsToVerify)
		{
			if(results.getEntropyStore(name) != null)
			{
				switch (name) 
				{
					case DESC:
						HashMap<Tuple<Resource, RDFNode>,Integer> descMultisetTest = Multisets.descMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(descMultisetTest, results, EntropiesEnum.DESC);
						break;
					case CLASSIF:
						HashMap<Tuple<Resource, RDFNode>,Integer> classifMultisetTest = Multisets.classifMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(classifMultisetTest, results, EntropiesEnum.CLASSIF);
						break;
					case DESCM:
						HashMap<Tuple<Resource, RDFNode>,Integer> descmMultisetTest = Multisets.descmMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(descmMultisetTest, results, EntropiesEnum.DESCM);
						break;
					case DESCMP:
						HashMap<Resource,Integer> descmpMultisetTest = Multisets.descmpMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(descmpMultisetTest, results, EntropiesEnum.DESCMP);
						break;
					case ECONN:
						HashMap<Resource,Integer> econnMultisetTest = Multisets.econnMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(econnMultisetTest, results, EntropiesEnum.ECONN);
						break;
					case RESOURCE:
						HashMap<Resource,Integer> resourceMultisetTest = Multisets.resourceMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(resourceMultisetTest, results, EntropiesEnum.RESOURCE);
						break;
					case SUBJECT:
						HashMap<Resource,Integer> subjectMultisetTest = Multisets.subjectMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(subjectMultisetTest, results, EntropiesEnum.SUBJECT);
						break;
					case PREDICATE:
						HashMap<Resource,Integer> predicateMultisetTest = Multisets.predicateMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(predicateMultisetTest, results, EntropiesEnum.PREDICATE);
						break;
					case LITERAL:
						HashMap<RDFNode,Integer> literalMultisetTest = Multisets.literalMultiset(sellerKG);
						allcorrect = allcorrect & checkSellerHashMap(literalMultisetTest, results, EntropiesEnum.LITERAL);
						break;
					default:
						logger.info("Name does not exist. Missing case.");
				}
			}
		}
		logger.info("Done testing Seller HashMaps.");
		logger.info("Done testing Entropies.");
		return allcorrect;
	}
	
	/**
	 * Computes entropy on multiset and checks if it matches the one stored in results
	 * 
	 * @param multiset to compute entropy on
	 * @param results holds entropy results
	 * @param name which entropy is checked
	 * @return true if value is correct, false otherwise
	 */
	private static <K> boolean checkEntropyValue(HashMap<K, Integer> multiset, EntropyResults results, EntropiesEnum name)
	{
		Logger logger = Log.getLogger();
		
		LinkedList<Integer> counts = new LinkedList<Integer>(multiset.values());
		double entropy = Entropy.calculateEntropy(counts);
		if(entropy == results.getEntropyStore(name).getEntropy())
		{
			logger.info(name.toString() + " entropy from protocol matches with true entropy");
			return true;
		} else
		{
			logger.info(name.toString() + " entropy does not match. \n"
					+ "Entropy calculated in the protocol: " + results.getEntropyStore(name).getEntropy() + "\n"
					+ "True Entropy: " + entropy);
			return false;
		}
	}
	
	/**
	 * Checks if HashMap from results matches HashMap computed from sellerMultisetTest
	 * Verifies that the seller computed his HashMap correctly
	 * 
	 * @param sellerMultisetTest true multiset (computed by buyer test)
	 * @param results store for entropy results
	 * @param name which entropy is beeing tested
	 * @return true if multisets match, false otherwise
	 */
	private static <K> boolean checkSellerHashMap(HashMap<K, Integer> sellerMultisetTest, EntropyResults results, EntropiesEnum name)
	{
		Logger logger = Log.getLogger();
		
		HashMap<String, Integer> signedDescMultisetTest = CBFSeller.multisetToCBF(sellerMultisetTest);
		HashMap<String, Integer> signedDescMultisetProtocol = results.getEntropyStore(name).getSellerMultiset();
		if(signedDescMultisetTest.equals(signedDescMultisetProtocol))
		{
			logger.info("Test " + name.toString() + " multiset matches the multiset resulting from the protocol.");
			return true;
		} else 
		{
			logger.info("Test " + name.toString() + " multiset does NOT match the multiset resulting from the protocol.");
			return false;
		}
	}
	
	/**
	 * Tests if oblivious transfer step has been done correctly
	 * Tests if all secrets ie model parts can be decrypted and are model parts
	 * Tests if all model parts together result in the full sellers model
	 * Tests if partitioning of seller model has been done correctly
	 * 
	 * @param sellerKG model used by seller instance
	 * @param encryptedParts AES encrypted model parts the seller produced in the oblivious transfer step
	 * @param keysBI AES keys the seller used to encrypt the model parts
	 * @param iv InitialisationVector used for CBC in AES Encryption
	 * @return true if all tests have been passed, false otherwise
	 */
	public static boolean testOblivousTransfer(Model sellerKG, LinkedList<byte[]> encryptedParts, LinkedList<BigInteger> keysBI, IvParameterSpec iv)
	{
		Logger logger = Log.getLogger();
		logger.info("Start testing oblivious transfer.");
		
		boolean allcorrect = true;
		
		logger.info("Start testing if all secrets can be decrypted.");
		LinkedList<Model> obliviousTransferSecrets = new LinkedList<Model>();
		int unableToDecrypt = 0;
		for(int i = 0; i < encryptedParts.size(); i++)
		{
			BigInteger keyBI = keysBI.get(i);
			try {
				SecretKeySpec secretKey = SellerPrepareKG.bigIntegerToAESKey(keyBI);
				String decryptedString = BuyerObtainKGPart.decrypt(encryptedParts.get(i), secretKey, iv);
				Model kgPart = ModelTools.stringToModel(decryptedString);
				if(kgPart != null)
				{
					obliviousTransferSecrets.add(kgPart);
				} else
				{
					unableToDecrypt++;
				}
			} catch (NoSuchAlgorithmException e) {
				logger.info("Exception when trying to decrypt oblivious transfer secrets during verification step. \n"
						+ e.getMessage() + "\n"
						+ e.toString());
			}
		}
		logger.info("Done decrypting secrets. Number of secrets that could not be decrypted: " + unableToDecrypt);
		allcorrect = (unableToDecrypt == 0);
		
		// -------------- Check if OT secrets add up to the Full Buyer Graph.
		logger.info("Start checking if combination of secrets match the full Sellers model.");
		Model otModel = ModelFactory.createDefaultModel();
		for(Model secret : obliviousTransferSecrets)
		{
			otModel.add(secret);
		}
		if(otModel.isIsomorphicWith(sellerKG))
		{
			logger.info("The combination of all secrets of the oblivious transfer equals the full model of the Seller.");
		} else
		{
			logger.info("The combination of all secrets of the oblivious transfer is not equal to the full model of the Seller.");
			allcorrect = false;
		}
		
		// -------------- Check if partitioning matches Buyer partitioning ------------------
		logger.info("Start testing if partitionings match.");

		LinkedList<Model> buyerPartitioning = Partitioning.partitionModel(sellerKG);

		if(buyerPartitioning.size() == obliviousTransferSecrets.size())
		{
			logger.info("Number of partitions and number of secrets match. Both are: " + buyerPartitioning.size());
		} else
		{
			logger.info("Number of partitions and number of secrets don't match. Number of partitions: " + buyerPartitioning.size() + " Number of secrets: " + obliviousTransferSecrets.size());
			allcorrect = false;
		}
		
		logger.info("Done testing oblivious transfer.");
		return allcorrect;
	}

}
