package privatesetintersection;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import protocol.Log;

public class KGIntersectionSeller
{	
	/**
	 * Creates a Bloom filter and stores a list of strings in it
	 * @param fpp false positive probability for Bloom filter
	 * @param signatures to be stored in the Bloom filter
	 * @return Bloom filter trained with given list 
	 */
	public static BloomFilter<String> trainBloomFilter(double fpp, LinkedList<String> signatures)
	{
		Logger logger = Log.getLogger();
		logger.info("Start building Bloom filter.");
		
		BloomFilter<String>bf = setupBloomFilter(signatures.size(), fpp);
		
		logger.info("Start adding signatures to BF.");
		for(String signature : signatures) 
		{
			bf.put(signature);
		}
		
		// Output that training of BF is done
		logger.info("Done training Bloom Filter. Elements contained in BF: " + bf.approximateElementCount());
		
		return bf;
	}

	/**
	 * Creates a Bloom filter
	 * @param size Number of elements to be stored in the Bloom filter
	 * @param fpp false positive probability of the Bloom filter
	 * @return Bloom filter
	 */
	public static BloomFilter<String> setupBloomFilter(long size, double fpp)
	{
		// Define how to enter an object into the BF (in this case a string)
		Funnel<String> statementSignatureFunnel = new Funnel<String>() 
		{
			  private static final long serialVersionUID = 1;
			  @Override
			  public void funnel(String statementSignature, PrimitiveSink into) 
			  {
			    into
			    	.putString(statementSignature, StandardCharsets.UTF_8);
			  }
		};
			
		// Create the bloom filter
		BloomFilter<String> bf = BloomFilter.create(statementSignatureFunnel, size, fpp);
		
		return bf;
	}
}
