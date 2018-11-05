package privatesetintersection;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.google.common.hash.BloomFilter;

import protocol.Log;
import protocol.Tuple;

public class KGIntersectionBuyer 
{
	/**
	 * Determines the intersection between two sets of statements. One set is represented by a Bloom filter and one by a list of signatures.
	 * @param bf Bloom filter representing one set
	 * @param signatures of second set
	 * @param numberStatementMap mapping from signatures to original elements
	 * @return intersection as Model
	 */
	public static Model determineIntersection(BloomFilter<String> bf, LinkedList<Tuple<String, Long>> signatures, HashMap<Long, Statement> numberStatementMap)
	{
		Logger logger = Log.getLogger();
		logger.info("Start determining intersection from BF. Compare signatures.");
		
		Model intersection = ModelFactory.createDefaultModel();
		
		ExecutorHandling.calculateParallelForListElements(signatures, (signature) -> 
		{
			if(bf.mightContain(signature.x))
			{
				return numberStatementMap.get(signature.y);
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
					logger.info("Exception in KGIntersectionBuyer determineIntersection, when trying to read features of parallel computation. \n" 
							+ e.getMessage() + "\n"
							+ e.toString() + "\n"
							+ "Protocol is being terminated.");
					System.exit(1);
				}
		});
		
		logger.info("Done determining intersection. Size: " + intersection.size());
		
		return intersection;
	}
	
	/**
	 * Stores statements contained in a model in a HashMap as values. The key is a unique identifier for each statement (long).
	 * @param model
	 * @return HashMap contianing mapping from identifier to statement
	 */
	public static HashMap<Long, Statement> numberStatements(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start numbering Buyer statements.");
		
		HashMap<Long, Statement> numberStatementMap = new HashMap<Long, Statement>((int) model.size());

		long statementNumber = 0;
		
		// Iterator over all statements
		StmtIterator statements = model.listStatements();
		
		// Go over all statements
		// For each statement create a new tuple with the statement and the number and add this to the list
		// then increase the number
		while(statements.hasNext())
		{
			Statement statement = statements.nextStatement();
			numberStatementMap.put(statementNumber, statement);
			statementNumber++;
		}
		
		logger.info("Done numbering Buyer statements.");
		return numberStatementMap;
	}

}
