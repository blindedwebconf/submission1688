package statistics;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import protocol.Log;
import protocol.Tuple;

/**
 * This class is used to compute multisets (represented as HashMaps) for various entropies on a model
 * @author ---
 *
 */
public class Multisets {
	
	/**
	 * Computes the multiset for Desc entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Desc multiset contains all predicate object combinations in the model
	 * @param model
	 * @return multiset as HashMap<Tuple<Resource,RDFNode>,Integer>
	 */
	public static HashMap<Tuple<Resource,RDFNode>,Integer> descMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Desc multiset.");
		
		HashMap<Tuple<Resource,RDFNode>,Integer> descMultiset = new HashMap<Tuple<Resource,RDFNode>,Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements().toList();
		
		for(Statement statement : statements)
		{
			Tuple<Resource, RDFNode> key = new Tuple<Resource, RDFNode>(statement.getPredicate(),statement.getObject());
			if(descMultiset.containsKey(key))
			{
				int count = descMultiset.get(key) + 1;
				descMultiset.put(key, count);
			} else
			{
				descMultiset.put(key, 1);
			}
		}
		
		logger.info("Size of desc: " + descMultiset.size());
		logger.info("Done computing Desc multiset.");
		return descMultiset;
	}
	
	/**
	 * Computes the multiset for Classif entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Classif multiset contains all predicate object combinations where the predicate is RDF.type
	 * @param model
	 * @return multiset as HashMap<Tuple<Resource,RDFNode>,Integer>
	 */
	public static HashMap<Tuple<Resource,RDFNode>,Integer> classifMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Classif multiset.");
		
		HashMap<Tuple<Resource,RDFNode>,Integer> classifMultiset = new HashMap<Tuple<Resource,RDFNode>,Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements(null, RDF.type, (RDFNode) null).toList();
//		Property rdfType = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
//		List<Statement> statements = model.listStatements(null, rdfType, (RDFNode) null).toList();
		
		for(Statement statement : statements)
		{
			Tuple<Resource, RDFNode> key = new Tuple<Resource, RDFNode>(statement.getPredicate(),statement.getObject());
			if(classifMultiset.containsKey(key))
			{
				int count = classifMultiset.get(key) + 1;
				classifMultiset.put(key, count);
			} else
			{
				classifMultiset.put(key, 1);
			}
		}
		
		logger.info("Size of classif: " + classifMultiset.size());
		logger.info("Done computing Classif multiset.");
		return classifMultiset;
	}
	
	/**
	 * Computes the multiset for Descm entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Descm multiset contains all predicate object combinations where the predicate is not RDF.type
	 * @param model
	 * @return multiset as HashMap<Tuple<Resource,RDFNode>,Integer>
	 */
	public static HashMap<Tuple<Resource,RDFNode>,Integer> descmMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Descm multiset.");
		
		HashMap<Tuple<Resource,RDFNode>,Integer> descmMultiset = new HashMap<Tuple<Resource,RDFNode>,Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements().toList();
		
		for(Statement statement : statements)
		{
			if(!statement.getPredicate().equals(RDF.type))
			{
				Tuple<Resource, RDFNode> key = new Tuple<Resource, RDFNode>(statement.getPredicate(),statement.getObject());
				if(descmMultiset.containsKey(key))
				{
					int count = descmMultiset.get(key) + 1;
					descmMultiset.put(key, count);
				} else
				{
					descmMultiset.put(key, 1);
				}
			}
		}
		
		
		logger.info("Found " + descmMultiset.size() + " Elements in the Hashmap.");
		logger.info("Done computing Descm multiset.");
		return descmMultiset;
	}
	
	/**
	 * Computes the multiset for Descmp entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Descmp multiset contains all predicate that are not RDF.type
	 * @param model
	 * @return multiset as HashMap<RDFNode, Integer>
	 */
	public static HashMap<Resource,Integer> descmpMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Descmp multiset.");
		
		HashMap<Resource,Integer> descmpMultiset = new HashMap<Resource,Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements().toList();
		
		for(Statement statement : statements)
		{
			if(!statement.getPredicate().equals(RDF.type))
			{
				Resource key = statement.getPredicate();
				if(descmpMultiset.containsKey(key))
				{
					int count = descmpMultiset.get(key) + 1;
					descmpMultiset.put(key, count);
				} else
				{
					descmpMultiset.put(key, 1);
				}
			}
		}
		
		
		logger.info("Found " + descmpMultiset.size() + " Elements in the Hashmap.");
		logger.info("Done computing Descmp multiset.");
		return descmpMultiset;
	}
	
	/**
	 * Computes the multiset for Econn entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Econn multiset contains all objects that are not literals (that are resources)
	 * @param model
	 * @return multiset as HashMap<Resource, Integer>
	 */
	public static HashMap<Resource, Integer> econnMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Econn multiset.");
		
		HashMap<Resource, Integer> econnMultiset = new HashMap<Resource, Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements().toList();
		
		for(Statement statement : statements)
		{
			if(statement.getObject().isResource())
			{
				Resource key = (Resource) statement.getObject();
				if(econnMultiset.containsKey(key))
				{
					int count = econnMultiset.get(key) + 1;
					econnMultiset.put(key, count);
				} else
				{
					econnMultiset.put(key, 1);
				}
			}
			
		}
		
		logger.info("Size of econn: " + econnMultiset.size());
		logger.info("Done computing Econn multiset.");
		return econnMultiset;
	}
	
	/**
	 * Computes the multiset for Resource entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Resource multiset contains all resources
	 * @param model
	 * @return multiset as HashMap<Resource, Integer>
	 */
	public static HashMap<Resource, Integer> resourceMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start calculating Resource multiset.");
		
		HashMap<Resource, Integer> resourceMultiset = new HashMap<Resource, Integer>((int) model.size());
		StmtIterator statements = model.listStatements();
		
		while(statements.hasNext())
		{
			Statement statement = statements.nextStatement();
			Resource subject = statement.getSubject();
			Resource predicate = statement.getPredicate();
			RDFNode object = statement.getObject();
			
			if(resourceMultiset.containsKey(subject))
			{
				int count = resourceMultiset.get(subject) + 1;
				resourceMultiset.put(subject, count);
			} else
			{
				resourceMultiset.put(subject, 1);
			}
			
			if(resourceMultiset.containsKey(predicate))
			{
				int count = resourceMultiset.get(predicate) + 1;
				resourceMultiset.put(predicate, count);
			} else
			{
				resourceMultiset.put(predicate, 1);
			}
			
			if(object.isResource() && resourceMultiset.containsKey(object))
			{
				int count = resourceMultiset.get((Resource) object) + 1;
				resourceMultiset.put((Resource) object, count);
			} else if(object.isResource())
			{
				resourceMultiset.put((Resource) object, 1);
			}
		}
		
		logger.info("Size of resource multiset: " + resourceMultiset.size());
		logger.info("Done computing Resource multiset.");
		return resourceMultiset;
	}
	
	/**
	 * Computes the multiset for Subject entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Subject multiset contains all subjects
	 * @param model
	 * @return multiset as HashMap<Resource, Integer>
	 */
	public static HashMap<Resource, Integer> subjectMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Subject multiset.");
		
		HashMap<Resource, Integer> subjectMultiset = new HashMap<Resource, Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements().toList();
		
		for(Statement statement : statements)
		{
			Resource key = statement.getSubject();
			if(subjectMultiset.containsKey(key))
			{
				int count = subjectMultiset.get(key) + 1;
				subjectMultiset.put(key, count);
			} else
			{
				subjectMultiset.put(key, 1);
			}
		}
		
		logger.info("Size of subject multiset: " + subjectMultiset.size());
		logger.info("Done computing Subject multiset.");
		return subjectMultiset;
	}
	
	/**
	 * Computes the multiset for Predicate entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Predicate multiset contains all predicates
	 * @param model
	 * @return multiset as HashMap<Resource, Integer>
	 */
	public static HashMap<Resource, Integer> predicateMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Predicate multiset.");
		
		HashMap<Resource, Integer> predicateMultiset = new HashMap<Resource, Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements().toList();
		
		for(Statement statement : statements)
		{
			Resource key = statement.getPredicate();
			if(predicateMultiset.containsKey(key))
			{
				int count = predicateMultiset.get(key) + 1;
				predicateMultiset.put(key, count);
			} else
			{
				predicateMultiset.put(key, 1);
			}
		}
		
		logger.info("Size of predicate multiset: " + predicateMultiset.size());
		logger.info("Done computing Predicate multiset.");
		return predicateMultiset;
	}
	
	/**
	 * Computes the multiset for Literal entropy
	 * HashMap contains element as key and how often it appears in the model as value
	 * Literal multiset contains all literals (objects that are not Resource)
	 * @param model
	 * @return multiset as HashMap<RDFNode, Integer>
	 */
	public static HashMap<RDFNode, Integer> literalMultiset(Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start computing Literal multiset.");
		
		HashMap<RDFNode, Integer> literalMultiset = new HashMap<RDFNode, Integer>((int) model.size());
		
		List<Statement> statements = model.listStatements().toList();
		
		for(Statement statement : statements)
		{
			RDFNode key = statement.getObject();
			if(literalMultiset.containsKey(key))
			{
				int count = literalMultiset.get(key) + 1;
				literalMultiset.put(key, count);
			} else if(key.isLiteral())
			{
				literalMultiset.put(key, 1);
			}
		}
		
		logger.info("Size of literal multiset: " + literalMultiset.size());
		logger.info("Done computing Literal multiset.");
		return literalMultiset;
	}
}
