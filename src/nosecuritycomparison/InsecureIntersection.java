package nosecuritycomparison;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;

import com.google.common.hash.BloomFilter;

import privatesetintersection.ExecutorHandling;
import privatesetintersection.KGIntersectionSeller;
import protocol.Log;

/**
 * Calculating the intersection the same way one in PSI but without any security. 
 * @author ---
 *
 */
public class InsecureIntersection {
	
	/**
	 * Trains a Bloom filter with the statements of the given model.
	 * Statements are encoded as strings before being stored in the Bloom filter
	 * @param model
	 * @return BloomFilter<String> storing statements of model
	 */
	public static BloomFilter<String> trainBloomFilter(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Starting Intersection without privacy. \n"
				+ "Start training Bloom filter.");
		
		BloomFilter<String> bf = KGIntersectionSeller.setupBloomFilter(model.size(), 0.03);
		
		List<Statement> statements = model.listStatements().toList();
		for(Statement statement : statements)
		{
			bf.put(statement.toString());
		}
		logger.info("Done training Bloom Filter. Elements contained in BF: " + bf.approximateElementCount());
		
		return bf;
	}
	
	/**
	 * Finds intersection between Model and Bloom filter
	 * Tests which statements of the model are present in the Bloom filter
	 * 
	 * @param model
	 * @param bf Bloom filter
	 * @return
	 */
	public static Model findIntersection(Model model, BloomFilter<String> bf)
	{
		Logger logger = Log.getLogger();
		logger.info("Start checking Statements for membership in Bloom fiter to find intersection.");
		
		Model intersection = ModelFactory.createDefaultModel();
		List<Statement> statements = model.listStatements().toList();
		ExecutorHandling.calculateParallelForListElements(statements, (statement) -> 
		{
			if(bf.mightContain(statement.toString()))
			{
				return statement;
			}
			return null;
		}, (Future future) -> 
		{
				Statement s;
				try {
					s = (Statement) future.get();
					if(s != null)
					{
						intersection.add(s);
					}
				} catch (InterruptedException | ExecutionException e) {
					 logger.info("Exception in insecure intersection, when buyer is testing his statements against the Bloom filter. \n" 
								+ e.getMessage() + "\n"
								+ e.toString());
				}
		});
		logger.info("Done determining intersection. Size: " + intersection.size());
		
		return intersection;
	}

}
