package nosecuritycomparison;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

public class InsecureEntropy {
	
	/**
	 * Calculates the counts for two multisets 'buyer' and 'seller'. 'Intersection' contains elements that are not supposed to be counted twice.
	 * No security is involved.
	 * @param buyer
	 * @param intersection
	 * @param seller
	 * @return
	 */
	public static <K> LinkedList<Integer> insecureUnifiedCount(HashMap<K, Integer> buyer, HashMap<K, Integer> intersection, HashMap<K, Integer> seller)
	{
		HashMap<K, Integer> unified = new HashMap<K, Integer>();
		
		for(Entry<K, Integer> entry : buyer.entrySet())
		{
			unified.put(entry.getKey(), entry.getValue());
		}
		
		for(Entry<K, Integer> entry : seller.entrySet())
		{
			if(buyer.containsKey(entry.getKey()))
			{
				int count = buyer.get(entry.getKey());
				count += entry.getValue();
				if(intersection.containsKey(entry.getKey()))
				{
					count = count - intersection.get(entry.getKey());
				}
				unified.put(entry.getKey(), count);
			} else
			{
				unified.put(entry.getKey(), entry.getValue());
			}
		}
		
		LinkedList<Integer> counts = new LinkedList<Integer>();
		
		for(Entry<K, Integer> entry : unified.entrySet())
		{
			counts.add(entry.getValue());
		}
		
		return counts;
	}

}
