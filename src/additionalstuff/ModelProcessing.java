package additionalstuff;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import protocol.Log;
import protocol.Tuple;
import statistics.Statistics;

/**
 * Class contains methods to alter the model.
 * These are not needed for the protocol but can be useful to create models for test cases
 * @author ---
 *
 */
public class ModelProcessing 
{
	/**
	 * Creates a new model containing the first n statments of the given model
	 * @param model
	 * @param n
	 * @return new model containing the first n statements of given model
	 */
	public static Model getFirstNStatements(Model model, Integer n)
	{
		if(n >= model.size())
		{
			return model;
		}
		
		Model smallModel = ModelFactory.createDefaultModel();
		
		StmtIterator statements = model.listStatements();
		
		int i = 0;
		while(i < n)
		{
			smallModel.add(statements.nextStatement());
			i++;
		}
		
		return smallModel;
	}

	// REQUIRES: a model, two percentages (floats) top and bot, boolean precise
	// Deletes all statements in with the top and bot (least) connected resources
	// If precise is true, cuts exactly the top and bot percentage of resources
	// If precise is false, and e.g. the bot percent connected resource has 3 statements
	// but there are more resources with also 3 statements which total to more then bot/top they still get cut
	// can influence index folder of model 
	// RETURNS: model (without most and least connected resources)
	public static Model cutMostAndLeastConnectedResources(Model model, float top, float bot, boolean precise)
	{
		// Get the logger
		Logger logger = Log.getLogger();
		
		if(bot < 0 || bot > 1 || top < 0 || top > 1)
		{
			logger.info("Bot and top need to be between 0 and 1. Model has not been cut.");
			return model;
		}
		
		//------------- Count the number of statements per resource ---------------------------
		// List of all resources
		HashSet<Resource> resourceSet = Statistics.findAllResources(model);
		LinkedList<Resource> resources = new LinkedList<Resource>(resourceSet);
		
		// Tuple storing number of statements for every resource
		LinkedList<Tuple<Resource, Integer>> resourceCountTuple = new LinkedList<Tuple<Resource, Integer>>();
		
		// Iterate over all resources
		// count the number of statements they appear in
		for(Resource r : resources)
		{
			// Get all statements where the resource is the subject
			List<Statement> statementsWithSubject = model.listStatements(r, null, (RDFNode) null).toList();
			// Get all statements where the resource is the object
			List<Statement> statementsWithObject = model.listStatements(null, null, r).toList();
			// Add the resource to the tuple with the number of found statements
			resourceCountTuple.add(new Tuple<Resource, Integer>(r, statementsWithSubject.size() + statementsWithObject.size()));
		}
		
		//--------------- Sort resources after number of statements -----------------------------
		// Sort list after number of Statements the Resource is contained in
		Collections.sort(resourceCountTuple, new Comparator<Tuple<Resource, Integer>>()
		{
			   @Override
			   public int compare(Tuple<Resource, Integer> tuple1, Tuple<Resource, Integer> tuple2){
			        if(tuple1.y < tuple2.y){
			           return -1; 
			        }
			        if(tuple1.y > tuple2.y){
			           return 1; 
			        }
			        return 0;
			   }
			});
		
		
		//-------------- Cut the top and bot resources ----------------------
		// Calculate the number of resources to cut
		int cutTop = Math.round(resourceCountTuple.size() * top);
		int cutBot = Math.round(resourceCountTuple.size() * bot);
		
		logger.info("Bottom Cut: " + resourceCountTuple.get(cutBot - 1).x + "      " + resourceCountTuple.get(cutBot - 1).y);
		logger.info("Top Cut: " + resourceCountTuple.get(resourceCountTuple.size() - cutTop - 1).x + "      " + resourceCountTuple.get(resourceCountTuple.size() - cutTop - 1).y);
		
		// If precise cut the exact number of resources
		if(precise)
		{
			// From the start of the sorted list remove the cutBot first resources
			for(int i = 0; i < cutBot; i++)
			{
				// Delete statements belonging to the resource
				model = deleteStatmentsOfResources(model, resourceCountTuple.getFirst().x);
				// Remove resource (tuple) from list
				resourceCountTuple.removeFirst();
			}
			
			// From the end of the sorted list remove the cutTop first resources
			for(int i = 0; i < cutTop; i++)
			{
				// Delete statements belonging to the resource
				model = deleteStatmentsOfResources(model, resourceCountTuple.getLast().x);
				// Remove resource (tuple) from list
				resourceCountTuple.removeLast();
			}
		} else // Precise = false
		{
			// Determine the minimum and maximum number of statements up to which a resource gets gut
			int botStatements = resourceCountTuple.get(cutBot - 1).y;
			int topStatements = resourceCountTuple.get(resourceCountTuple.size() - cutTop - 1).y;
			// Cut all resources with less or equal to minimum statements
			while(resourceCountTuple.getFirst().y <= botStatements)
			{
				// Delete statements belonging to the resource
				model = deleteStatmentsOfResources(model, resourceCountTuple.getFirst().x);
				// Remove resource (tuple) from list
				resourceCountTuple.removeFirst();
			}
			//Cut all resources with more or equal to maximum statements
			while(resourceCountTuple.getLast().y >= topStatements)
			{
				// Delete statements belonging to the resources
				model = deleteStatmentsOfResources(model, resourceCountTuple.getLast().x);
				// Remove resource (tuple) from list
				resourceCountTuple.removeLast();
			}
		}
		
		return model;
	}
	
	// REQUIRES: model, resource
	// Deletes all statements from the model where the resources is the subject or object
	private static Model deleteStatmentsOfResources(Model model, Resource resource)
	{
		// Find all statements where resource is the subject
		List<Statement> statementsWithSubject = model.listStatements(resource, null, (RDFNode) null).toList();
		// Find all statements where resource is the object
		List<Statement> statementsWithObject = model.listStatements(null, null, resource).toList();
		// Remove all statements where resource is the subject
		model.remove(statementsWithSubject);
		// Remove all statements where resource is the object
		model.remove(statementsWithObject);
		return model;
	}
	
	public static Model generateSubModel(LinkedList<Model> partitioning, double percentage)
	{
		Model subModel = ModelFactory.createDefaultModel();
//		LinkedList<Model> partitioning = Partitioning.partitionDBSCAN(model);
		for(Model part : partitioning)
		{
			double random = Math.random();
			if(random <= percentage)
			{
				subModel.add(part);
			}
		}
		return subModel;
	}
	
	
}
