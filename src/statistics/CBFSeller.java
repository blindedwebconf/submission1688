package statistics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Logger;

import privatesetintersection.BuyerBlindSignatures;
import privatesetintersection.SellerBlindSignatures;
import protocol.Log;
import protocol.Tuple;

public class CBFSeller {
	
	
	/**
	 * Calculates blind signatures for keys of a multiset. 
	 * Then stores signatures and values in a CBF (HashMap)
	 * @param multiset
	 * @return HashMap containing signatures as key and multiset value as  value
	 */
	public static <K> HashMap<String, Integer> multisetToCBF(HashMap<K, Integer> multiset)
	{
		Logger logger = Log.getLogger();
		
		HashMap<String, Integer> signedMultiset = new HashMap<String, Integer>(multiset.size());
		logger.info("Counting Bloom Filter has been set up.");
		
		// --------------------- Sign Elements ------------------------
		// Get a HashMap containing a mapping from identifier to plain text element
		HashMap<Long, K> multisetIndex = numberElements(multiset);
		// Turns identifier HashMap into a list with identifiers and elements
		LinkedList<Tuple<K,Long>> multisetIndexList = BuyerBlindSignatures.hashmapToTupleList(multisetIndex);
		// Sign elements
		LinkedList<Tuple<Long,String>> blindSignatures = SellerBlindSignatures.signSellerMultiset(multisetIndexList);
		
		// --------------------- Create HashMap with signatures and counts --------------------
		logger.info("Start training Counting Bloom Filter.");
		for(Tuple<Long,String> signature : blindSignatures)
		{
			// Use the HashMap containing the identifiers and plain text elements to find the element the signatures belongs to
			// Use this to look up the count the signature belongs to
			// Store both into the HashMap (signature as key, count as value)
			signedMultiset.put(signature.y, multiset.get(multisetIndex.get(signature.x)));
		}
		
		logger.info("Done training Counting Bloom Filter. Elements in the Counting Bloom Filter: " + signedMultiset.size());
		
		return signedMultiset;
	}
	
	
	/**
	 * Matches each key of a HashMap with a unique identifier.
	 * @param multiset
	 * @return HashMap with identifier as key and key of original map as value
	 */
	public static <K> HashMap<Long, K> numberElements(HashMap<K, Integer> multiset)
	{
		HashMap<Long, K> multisetIndex = new HashMap<Long, K>();
		
		// Counting up for each element, used as unique identifier 
		long i = 0;
		for(Entry<K,Integer> entry : multiset.entrySet())
		{
			multisetIndex.put(i, entry.getKey());
			i++;
		}
		
		return multisetIndex;
	}
}
