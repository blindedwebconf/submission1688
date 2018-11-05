package statistics;

import java.util.LinkedList;
import java.util.logging.Logger;

import protocol.Log;

public class Entropy {

	/**
	 * Calculates Shannon entropy for given counts
	 * @param counts of elements
	 * @return double entropy
	 */
	public static double calculateEntropy(LinkedList<Integer> counts)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing entropy for given counts. (only the actual math)");
		
		// Get the sum over all counts
		long sum = totalNumberOfElements(counts);
		// Store the entropy
		double entropy = 0;
		
		// Iterate over all counts
		for(int count : counts)
		{
			if(count > 0)
			{
				// Calculate the relative likelihood of an element
				double probability = (double) count/sum;
				// Calculate and add up the entropy for the element
				entropy += probability * logN(probability, 2);
			}
		}
		
		logger.info("Done computing entropy for given counts (only the acutal math). Entropy: " + -entropy);
		// negate entropy for positive value
		return -entropy;
	}
	
	/**
	 * Sums up all counts
	 * @param counts
	 * @return long total number of elements
	 */
	private static long totalNumberOfElements(LinkedList<Integer> counts)
	{
		// Stores sum
		long sum = 0;
		// Iterate over all elements of the list / all counts
		for(int count : counts)
		{
			// Add the count to the sum
			sum += count;
		}
		
		return sum;
	}
	
	/**
	 * Computes logarithm for arbitrary base
	 * @param x
	 * @param base
	 * @return logarithm of x to base base
	 */
	private static double logN(double x, double base)
	{
		return Math.log(x)/Math.log(base);
	}
}
