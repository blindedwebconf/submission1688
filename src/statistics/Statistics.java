package statistics;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import protocol.Log;

public class Statistics 
{

	/**
	 * Calculates:	Number of Statements contained in the model
	 *				Number of different Resources contained in the model
	 *				Number of different Subjects contained in the model
	 *				Number of different Predicates contained in the model
	 *				Number of different Objects (Resources + Literals) contained in the model
	 *				Number of different Resource Objects contained in the model
	 *				Number of different Literal Objects contained in the model
	 *				Average number of outgoing links per Subject 
	 *				Average number of incoming links per Object Resource
	 *				Average number of Literals per Subject 
	 *				Average number of Resource Objects per Subject 
	 *				Minimum, maximum, 25%, 50%, 75% quantile for the 4 averages above
	 * @param model
	 * @return StatistcsResults storing results for all these statistics
	 */
	public static StatisticsResults statistics(Model model)
	{
		StatisticsResults results = new StatisticsResults();
		
		// Size of the model = Number of Statements contained in the model
		long nrStatements = model.size();
		results.setSize(nrStatements);
		
		// Number of Resources in model
		long resources = findAllResources(model).size();
		results.setResources(resources);
		
		// Number of different Resources that appear as Subject in the model
		long nrSubjects = model.listSubjects().toList().size();
		results.setSubjects(nrSubjects);
		
		// Number of different Objects (Resources and Literals)
		List<RDFNode> objects = model.listObjects().toList();
		results.setObjects(objects.size());

		// The above number of Objects does not distinguish between Resources and Literals
		// Count these Resources and Literals individually
		int resourceCounter = 0;		// Number of different Resources that appear as Objects
		int literalCounter = 0;			// Number of different Literals that appear as Objects
		for(RDFNode o : objects)
		{
			if(o.isResource())
			{
				resourceCounter++;
			}
			else if(o.isLiteral())
			{
				literalCounter++;
			}
		}
		results.setObjectResources(resourceCounter);
		results.setLiterals(literalCounter);
		
		// Average number of outgoing Links
		results.setAvgOutgoingLinks(((double) nrStatements)/nrSubjects);
		
		// Above looked at number of different Objects
		// Now count the total number of Resources as Objects
		// and total number of Literals as Objects
		// Count number of different predicates
		HashSet<Resource> predicates = new HashSet<Resource>();
		long nrStatementsWithResourceObject = 0;
		long nrStatementsWithLiteralObject = 0;
//		List<Statement> statements = model.listStatements().toList();
//		for(Statement s : statements)
		StmtIterator statements = model.listStatements();
		while(statements.hasNext())
		{
			Statement s = statements.nextStatement();
			if(s.getObject().isResource())
			{
				nrStatementsWithResourceObject++;
			}
			else if(s.getObject().isLiteral())
			{
				nrStatementsWithLiteralObject++;
			}
			if(!predicates.contains(s.getPredicate()))
			{
				predicates.add(s.getPredicate());
			}
		}
		results.setAvgIncomingLinks(((double) nrStatementsWithResourceObject)/resourceCounter);
		results.setAvgLiterals(((double) nrStatementsWithLiteralObject)/nrSubjects);
		results.setAvgObjectResources(((double) nrStatementsWithResourceObject)/nrSubjects);
		results.setTotalObjectResources(nrStatementsWithResourceObject);
		results.setTotalLiterals(nrStatementsWithLiteralObject);
		results.setPredicates(predicates.size());
		
		
		// Calculate min, max, 25%, 50% and 75% quantile for averages
		LinkedList<Integer> subjectStatementCount = new LinkedList<Integer>();
		LinkedList<Integer> subjectResObjectCount = new LinkedList<Integer>();
		LinkedList<Integer> subjectLiteralCount = new LinkedList<Integer>();
		ResIterator subjects = model.listSubjects();
		while(subjects.hasNext())
		{
			Resource subject = subjects.nextResource();
			StmtIterator statementsWithSubject = model.listStatements(subject, null, (RDFNode) null);
			int count = 0;
			int countObjectIsResource = 0;
			int countObjectIsLiteral = 0;
			while(statementsWithSubject.hasNext())
			{
				Statement statement = statementsWithSubject.nextStatement();
				count++;
				if(statement.getObject().isResource())
				{
					countObjectIsResource++;
				}
				if(statement.getObject().isLiteral())
				{
					countObjectIsLiteral++;
				}
			}
			subjectStatementCount.add(count);
			subjectResObjectCount.add(countObjectIsResource);
			subjectLiteralCount.add(countObjectIsLiteral);
		}
		
		subjectStatementCount = sortIntList(subjectStatementCount);
		subjectResObjectCount = sortIntList(subjectResObjectCount);
		subjectLiteralCount = sortIntList(subjectLiteralCount);
		
		results.setMinOutgoingLinks(subjectStatementCount.getFirst());
		results.setMaxOutgoingLinks(subjectStatementCount.getLast());
		Double outLinks25Index = subjectStatementCount.size()*0.25;
		results.setOutgoingLinks25(subjectStatementCount.get(outLinks25Index.intValue()));
		Double outLinks50Index = subjectStatementCount.size()*0.5;
		results.setOutgoingLinks50(subjectStatementCount.get(outLinks50Index.intValue()));
		Double outLinks75Index = subjectStatementCount.size()*0.75;
		results.setOutgoingLinks75(subjectStatementCount.get(outLinks75Index.intValue()));
		
		results.setMinResObjects(subjectResObjectCount.getFirst());
		results.setMaxResObjects(subjectResObjectCount.getLast());
		Double resObject25Index = subjectResObjectCount.size()*0.25;
		results.setResObjects25(subjectResObjectCount.get(resObject25Index.intValue()));
		Double resObject50Index = subjectResObjectCount.size()*0.50;
		results.setResObjects50(subjectResObjectCount.get(resObject50Index.intValue()));
		Double resObject75Index = subjectResObjectCount.size()*0.75;
		results.setResObjects75(subjectResObjectCount.get(resObject75Index.intValue()));
		
		results.setMinLiterals(subjectLiteralCount.getFirst());
		results.setMaxLiterals(subjectLiteralCount.getLast());
		Double literal25Index = subjectLiteralCount.size()*0.25;
		results.setLiterals25(subjectLiteralCount.get(literal25Index.intValue()));
		Double literal50Index = subjectLiteralCount.size()*0.50;
		results.setLiterals50(subjectLiteralCount.get(literal50Index.intValue()));
		Double literal75Index = subjectLiteralCount.size()*0.75;
		results.setLiterals75(subjectLiteralCount.get(literal75Index.intValue()));
		
		LinkedList<Integer> objectStatementCount = new LinkedList<Integer>();
		NodeIterator objectsIterator = model.listObjects();
		while(objectsIterator.hasNext())
		{
			RDFNode object = objectsIterator.nextNode();
			if(object.isResource())
			{
				objectStatementCount.add(model.listStatements(null, null, object).toList().size());
			}
		}
		
		objectStatementCount = sortIntList(objectStatementCount);
		
		results.setMinIncomingLinks(objectStatementCount.getFirst());
		results.setMaxIncomingLinks(objectStatementCount.getLast());
		Double inLinks25Index = objectStatementCount.size()*0.25;
		results.setIncomingLinks25(objectStatementCount.get(inLinks25Index.intValue()));
		Double inLinks50Index = objectStatementCount.size()*0.50;
		results.setIncomingLinks50(objectStatementCount.get(inLinks50Index.intValue()));
		Double inLinks75Index = objectStatementCount.size()*0.75;
		results.setIncomingLinks75(objectStatementCount.get(inLinks75Index.intValue()));
		
		return results;
	}
	
	/**
	 * Sorts a list of integers in increasing order
	 * @param list
	 * @return
	 */
	private static LinkedList<Integer> sortIntList(LinkedList<Integer> list)
	{
		// Sort LinkedList
		Collections.sort(list, new Comparator<Integer>()
		{
			   @Override
			   public int compare(Integer int1, Integer int2)
			   {
			        if(int1 < int2){
			           return -1; 
			        }
			        if(int1 > int2){
			           return 1; 
			        }
			        return 0;
			   }
		});
		return list;
	}
	
	/**
	 * Finds all resources used in the model.
	 * @param model
	 * @return HashSet containing all resources in the model
	 */
	public static HashSet<Resource> findAllResources(Model model)
	{
		HashSet<Resource> resourceSet = new HashSet<Resource>((int) model.size() * 3);
		StmtIterator statements = model.listStatements();
		
		while(statements.hasNext())
		{
			Statement statement = statements.nextStatement();
			Resource subject = statement.getSubject();
			Resource predicate = statement.getPredicate();
			RDFNode object = statement.getObject();
			
			if(!resourceSet.contains(subject))
			{
				resourceSet.add(subject);
			}
			if(!resourceSet.contains(predicate))
			{
				resourceSet.add(predicate);
			}
			if(object.isResource() && !resourceSet.contains(object))
			{
				resourceSet.add((Resource) object);
			}
		}
		
		return resourceSet;
	}
	
	/**
	 * Logs the given statistics results
	 * @param results
	 */
	public static void outputStatistics(StatisticsResults results)
	{
		Logger logger = Log.getLogger();
		
		logger.info("Size/number of statements of model: " + results.getSize() + "\n"
					+ "Number of different resources: " + results.getResources() + "\n"
					+ "Number of different subjects: " + results.getSubjects() + "\n"
					+ "Number of different predicates: " + results.getPredicates() + "\n"
					+ "Number of different objects: " + results.getObjects() + "\n"
					+ "Number of different object resources: " + results.getObjectResources() + "\n"
					+ "Number of different literals: " + results.getLiterals() + "\n"
					+ "Total number of object resources: " + results.getTotalObjectResources() + "\n"
					+ "Total number of literals: " + results.getTotalLiterals() + "\n"
					+ "Average number of outgoing links per subject: " + results.getAvgOutgoingLinks() + "\n"
					+ "Minimum number of outgoing links per subject: " + results.getMinOutgoingLinks() + "\n"
					+ "Maximum number of outgoing links per subject: " + results.getMaxOutgoingLinks() + "\n"
					+ "25% quantile of outgoing links per subject: " + results.getOutgoingLinks25() + "\n"
					+ "50% quantile of outgoing links per subject: " + results.getOutgoingLinks50() + "\n"
					+ "75% quantile of outgoing links per subject: " + results.getOutgoingLinks75() + "\n"
					+ "Average number of incoming links per object resource: " + results.getAvgIncomingLinks() + "\n"
					+ "Minimum number of incoming links per object resource: " + results.getMinIncomingLinks() + "\n"
					+ "Maximum number of incoming links per object resource: " + results.getMaxIncomingLinks() + "\n"
					+ "25% quantile of incoming links per object resrouce: " + results.getIncomingLinks25() + "\n"
					+ "50% quantile of incoming links per object resource: " + results.getIncomingLinks50() + "\n"
					+ "75% quantile of incoming links per object resource: " + results.getIncomingLinks75() + "\n"
					+ "Average number of literals per subject: " + results.getAvgLiterals() + "\n"
					+ "Minimum number of literals per subject: " + results.getMinLiterals() + "\n"
					+ "Maximum number of literals per subject: " + results.getMaxLiterals() + "\n"
					+ "25% quantile of literals per subject: " + results.getLiterals25() + "\n"
					+ "50% quantile of literals per subject: " + results.getLiterals50() + "\n"
					+ "75% quantile of literals per subject: " + results.getLiterals75() + "\n"
					+ "Average number of object resources per subject: " + results.getAvgObjectResources() + "\n"
					+ "Minimum number of object resources per subject: " + results.getMinResObjects() + "\n"
					+ "Maximum number of object resources per subject: " + results.getMaxResObjects() + "\n"
					+ "25% quantile of object resources per subject: " + results.getResObjects25() + "\n"
					+ "50% quantile of object resources per subject: " + results.getResObjects50() + "\n"
					+ "75% quantile of object resources per subject: " + results.getResObjects75() + "\n");
	}
}
