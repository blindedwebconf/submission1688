package additionalstuff;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import protocol.Log;
import protocol.Tuple;

public class NeighborOfNeighborAttack 
{

	//Takes a model
	//randomly picks ~ 90% of the statements to generate a smaller but similar model
	//Outputs how many of the missing statements could have been found by making a link from a Node to the neighbor of a neighbor
	public static void neighbourOfNeighbour (Model model, double intersectionSize, int numberPredicates)
	{
		Logger logger = Log.getLogger();
		
		Model subModel = randomSubModel(model, intersectionSize);
		
		LinkedList<Property> predicates = nMostFrequentPredicates(subModel, numberPredicates);
		
		logger.info("Size of the sub model: " + subModel.size());
		
		// List the statements in the sub model
		StmtIterator subIterator = subModel.listStatements();
		
		Model statementsFoundModel = ModelFactory.createDefaultModel();
		
		while(subIterator.hasNext())
		{
			Statement statement = subIterator.nextStatement();
			
			if(statement.getObject().isResource())
			{
				StmtIterator neighbourIterator = subModel.listStatements((Resource) statement.getObject(), null, (RDFNode) null);
				
				while(neighbourIterator.hasNext())
				{
					Statement neighbour = neighbourIterator.nextStatement();
					Resource subject = statement.getSubject();
					RDFNode object = neighbour.getObject();
					for(Property predicate : predicates)
					{
						Statement candidate = ResourceFactory.createStatement(subject, predicate, object);
						if(!subModel.contains(candidate) && model.contains(candidate))
						{
							statementsFoundModel.add(candidate);
						}
					}
				}
			}
		}
		
		logger.info("Statements missing: " + (model.size() - subModel.size()) + " Statements found: " + statementsFoundModel.size() + " Number of predicates used: " + predicates.size());
	}
	
	private static Model randomSubModel(Model model, double intersectionSize)
	{
		Logger logger = Log.getLogger();
		
		// List the statements in the model
		StmtIterator iterator = model.listStatements();
		
		double random = 0;
		Model subModel = ModelFactory.createDefaultModel();
		
		// Iterate over the model and randomly select statements for two sub models
		while(iterator.hasNext())
		{
			Statement statement = iterator.nextStatement();
			
			random = Math.random();
			
			if(random <= intersectionSize)
			{
				subModel.add(statement);
			}
		}
		
		logger.info("Done creating sub model.");
		
		return subModel;
	}
	
	private static LinkedList<Property> nMostFrequentPredicates(Model model, int n)
	{
		Logger logger = Log.getLogger();
		
		StmtIterator statements = model.listStatements();
		HashMap<Property, Integer> frequency = new HashMap<Property, Integer>();
		
		// Create HashMap containing mapping from predicate to its frequency
		while(statements.hasNext())
		{
			Statement statement = statements.nextStatement();
			Property predicate = statement.getPredicate();
			if(frequency.containsKey(predicate))
			{
				int count = frequency.get(predicate);
				frequency.put(predicate, count+1);
			} else
			{
				frequency.put(predicate, 1);
			}
		}
		
		// Turn HashMap into a LinkedList
		LinkedList<Tuple<Property, Integer>> frequencyList = new LinkedList<Tuple<Property, Integer>>();
		for(Entry<Property, Integer> entry : frequency.entrySet())
		{
			frequencyList.add(new Tuple<Property, Integer>(entry.getKey(), entry.getValue()));
		}
		
		// Sort LinkedList
		Collections.sort(frequencyList, new Comparator<Tuple<Property, Integer>>()
		{
			   @Override
			   public int compare(Tuple<Property, Integer> tuple1, Tuple<Property, Integer> tuple2)
			   {
			        if(tuple1.y < tuple2.y){
			           return 1; 
			        }
			        if(tuple1.y > tuple2.y){
			           return -1; 
			        }
			        return 0;
			   }
		});
		
		// Return first n predicates of list
		LinkedList<Property> predicates = new LinkedList<Property>();
		for(int i = 0; i < n; i++)
		{
			logger.info("Predicate frequency: " + frequencyList.getFirst().y);
			predicates.add(frequencyList.getFirst().x);
			frequencyList.removeFirst();
		}
		
		return predicates;
	}
}
