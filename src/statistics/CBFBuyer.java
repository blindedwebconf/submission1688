package statistics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Logger;

import privatesetintersection.BuyerBlindSignatures;
import protocol.Log;
import protocol.Tuple;

public class CBFBuyer {
	
	/**
	 * Obtains blind signatures for keys of a HashMap
	 * @param multiset HashMap to be signed
	 * @return HashMap containing signatures as keys and singed keys as values
	 */
	public static <K> LinkedList<Tuple<String, K>> getBuyerElementsSignatures(HashMap<K, Integer> multiset)
	{
		Logger logger = Log.getLogger();
		logger.info("Start getting signatures for Buyer multiset.");
		
		// --------------- Get blind signatures for Buyers multiset (just the keys) ----------------------
		// Get identifier for each key
		HashMap<Long, K> multisetIndex = CBFSeller.numberElements(multiset);
		// Get blind signatures for keys
		LinkedList<Tuple<String, Long>> blindSignatures = BuyerBlindSignatures.bobBlindSignature(multisetIndex);
		LinkedList<Tuple<String, K>> signaturesWithElements = matchSignaturesWithElements(blindSignatures, multisetIndex);
		
		return signaturesWithElements;
	}
	
	/**
	 * Combines the counts of the sellers and buyers multisets to one LinkedList of counts.
	 * @param multiset of the buyer
	 * @param multisetIntersection of the intersection between buyer and seller model
	 * @param signedSellerMultiset
	 * @param signaturesWithElements signature element HashMap of the buyer
	 * @return LinkedList of combined counts (without elements)
	 */
	public static <K> LinkedList<Integer> multisetIntersection(HashMap<K, Integer> multiset, HashMap<K, Integer> multisetIntersection, HashMap<String, Integer> signedSellerMultiset, LinkedList<Tuple<String, K>> signaturesWithElements)
	{
		Logger logger = Log.getLogger();
		logger.info("Start determining combined counts for both models.");
		
		// --------------- Calculate counts for the combined KGs -----------------------------------------
		// Get the counts of the Seller for elements that both the Seller and Buyer have
		LinkedList<Tuple<K, Integer>> commonElementsCount = intersectionWithCount(signedSellerMultiset, signaturesWithElements);
		// Add the count of the Seller for shared/common elements to the counts of the Buyer
		multiset = addCommonToMultiset(multiset, commonElementsCount);
		// Elements resulting from the intersection of the two KG are counted twice, undo this
		multiset = removeDuplicates(multiset, multisetIntersection);
		// Get the counts from the Buyer multiset and append the remaining Seller counts to the list
		HashMap<String, K> signaturesHashMap = signaturesToHashMap(signaturesWithElements);
		LinkedList<Integer> counts = joinedCountList(signedSellerMultiset, multiset, signaturesHashMap);
		
		logger.info("Done determining combined counts for both models.");
		return counts;
	}
	
	/**
	 * Matches signatures with their orignial elements using identifiers stored in multisetIndex.
	 * 
	 * @param blindSignatures of multiset keys
	 * @param multisetIndex multiset keys and their identifiers
	 * @return LinkedList of Tuples containing signature and element the signature belongs to
	 */
	private static <K> LinkedList<Tuple<String, K>> matchSignaturesWithElements(LinkedList<Tuple<String, Long>> blindSignatures, HashMap<Long, K> multisetIndex)
	{
		LinkedList<Tuple<String, K>> signaturesWithElements = new LinkedList<Tuple<String, K>>();
		for(Tuple<String, Long> signature : blindSignatures)
		{
			K element = multisetIndex.get(signature.y);
			signaturesWithElements.add(new Tuple<String, K>(signature.x, element));
		}
		
		return signaturesWithElements;
	}
	
	/**
	 * Finds common elements between the sellers and buyers multiset.
	 * Also finds how often it is contained in the sellers multiset.
	 * @param signedSellerMultiset
	 * @param signaturesWithElements of the buyers multiset
	 * @return LinkedList of Tuples with all element both the seller and buyers multisets contain and their value in the sellers multiset
	 */
	private static <K> LinkedList<Tuple<K, Integer>> intersectionWithCount(HashMap<String, Integer> signedSellerMultiset, LinkedList<Tuple<String, K>> signaturesWithElements)
	{
		LinkedList<Tuple<K, Integer>> intersection = new LinkedList<Tuple<K, Integer>>();
		
		for(Tuple<String, K> signature : signaturesWithElements)
		{
			// Check if the signature matches a signature of the Seller
			if(signedSellerMultiset.containsKey(signature.x))
			{
				// If a match is found add identifier with count to intersection list
				intersection.add(new Tuple<K, Integer>(signature.y, signedSellerMultiset.get(signature.x)));
			}
		}
		
		return intersection;
	}
	
	/**
	 * Adds the counts of the seller of the shared elements to the buyers multiset
	 * @param multiset of the buyer
	 * @param commonElementsCount shared elements between buyer and seller with seller counts
	 * @return multiset with the buyer counts + seller counts for shared elements
	 */
	private static <K> HashMap<K, Integer> addCommonToMultiset(HashMap<K, Integer> multiset, LinkedList<Tuple<K, Integer>> commonElementsCount)
	{
		for(Tuple<K, Integer> common : commonElementsCount)
		{
			// Get the Buyer count of the element
			int count = multiset.get(common.x);
			// Add the Seller count to the Buyer count
			count = count + common.y;
			// Put the element with the new count into the multiset
			multiset.put(common.x, count);
		}
		return multiset;
	}
	
	/**
	 * addCommonToMultiset adds the sellers counts for shared elements to the buyers counts.
	 * Elements lying in the intersection of the sellers and buyers model are now counted twice.
	 * This method undoes this.
	 * Counts of elements in the intersection get subtracted once.
	 * @param multisetWithCommon
	 * @param multisetIntersection
	 * @return multiset (HashMap) where elements from the intersection are no longer counted twice.
	 */
	private static <K> HashMap<K, Integer> removeDuplicates(HashMap<K, Integer> multisetWithCommon, HashMap<K, Integer> multisetIntersection)
	{
		for(Entry<K, Integer> entry : multisetIntersection.entrySet())
		{
			// Get the old count of the element
			int count = multisetWithCommon.get(entry.getKey());
			// Subtract the number of times it is in the intersection
			count = count - entry.getValue();
			
			// Store element with the new count into the hashmap (the multiset)
			multisetWithCommon.put(entry.getKey(), count);
		}
		
		return multisetWithCommon;
	}
	 
	/**
	 * Turns a LinkedList of tuples into a HashMap where Tuple.x are the keys and Tuple.y are the values.
	 * @param signaturesWithElements
	 * @return HashMap containing Tuples of LinkedList
	 */
	private static <K> HashMap<String, K> signaturesToHashMap(LinkedList<Tuple<String, K>> signaturesWithElements)
	{
		HashMap<String, K> signaturesHashMap = new HashMap<String, K>();
		for(Tuple<String, K> signature : signaturesWithElements)
		{
			signaturesHashMap.put(signature.x, signature.y);
		}
		return signaturesHashMap;
	}
	
	/**
	 * Calculates a list of counts for the unified multisets of the seller and buyer.
	 * Adds all counts of the buyer and shared elements to counts (from multisetWithCommon).
	 * Then checks for all seller elements if their signature is contained in signaturesHashMap (containing signatures of buyer elements).
	 * If not their count is added to the list of counts.
	 * @param sellerMultiset
	 * @param multisetWithCommon multiset containing buyers elements and counts + counts of the seller for shared elements - intersection duplicates
	 * @param signaturesHashMap signatures of the buyers elements
	 * @return LinkedList of counts
	 */
	private static <K> LinkedList<Integer> joinedCountList(HashMap<String, Integer> sellerMultiset, HashMap<K, Integer> multisetWithCommon, HashMap<String, K> signaturesHashMap)
	{
		LinkedList<Integer> counts = new LinkedList<Integer>();
		
		for(Entry<String, Integer> entry : sellerMultiset.entrySet())
		{
			if(!signaturesHashMap.containsKey(entry.getKey()))
			{
				counts.add(entry.getValue());
			}
		}
		
		for(Entry<K, Integer> entry : multisetWithCommon.entrySet())
		{
			// Add count to the list
			counts.add(entry.getValue());
		}
		
		return counts;
	}

}
