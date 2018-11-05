package knowledgegraphpartitioning;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import protocol.GetUserInput;
import protocol.Log;
import protocol.Seller;
import protocol.Tuple;
import statistics.StatisticsResults;

public class Partitioning 
{

	/**
	 * Asks the user for a partitioning strategy and partitions the model with it.
	 * @param model
	 * @return list of models, which is the partitioning of the original model
	 */
	public static LinkedList<Model> partitionModel(Model model)
	{
		Logger logger = Log.getLogger();
		
		Strategies partitioningStrategy = GetUserInput.askPartitioningStrategy();
		
		LinkedList<Model> partitions = new LinkedList<Model>();
		switch (partitioningStrategy)
		{
			case BALANCEDDBSCAN:
				partitions = Partitioning.partitionDBSCAN(model);
				break;
			case DBSCAN:
				int minStatements = GetUserInput.askDBSCANminStatements();
				partitions = Partitioning.dbscan(model, minStatements, false);
				break;
			case RANGE:
				int range = GetUserInput.askRange();
				partitions = Partitioning.partitionNeighborhood(model, range);
				break;
			case RESOURCE:
				partitions = Partitioning.partitionResorces(model);
				break;
			default:
				logger.info("Non existing patitioning strategy. Missing case.");
		}
		return partitions;
	}
	
	/**
	 * Partitions a model using a DBSCAN approach. Iteratively partitions each part again with a higher minstatements. Then balances the resulting parts using statements that not yet belong to a part.
	 * @param model
	 * @return partitioning = list of models
	 */
	public static LinkedList<Model> partitionDBSCAN (Model model)
	{		
		Logger logger = Log.getLogger();
		logger.info("Start DBSCAN partitioning with balancing.");
		
		if(model.isEmpty() || model == null)
		{
			logger.info("Cannot partition this model as it is empty or null.");
			return null;
		}
		
		int minPoints = 10;
		StatisticsResults statisticsResults = Seller.getStatisticsResults();
		if(!(statisticsResults == null))
		{
			minPoints = (int) statisticsResults.getOutgoingLinks25();
		}
		
		int level = 0;
		TreeNode<Model> dbscanResultsTree = new TreeNode<Model>(model);
		
		Boolean biggestUsableClusterFound = false;
		
		while(!biggestUsableClusterFound)
		{
			for(TreeNode<Model> node : dbscanResultsTree.getNodesOnLevel(level))
			{
				if(node.getData().size() > minPoints)
				{
					LinkedList<Model> dbscanResult = dbscan(node.getData(), minPoints, true);
					if(!dbscanResult.isEmpty())
					{
						for(Model c : dbscanResult) 
						{
							if(c.size() > 0)
							{
								node.addChild(c);
							}
						}
					}
				}
				
			}
			
			level++;
			
			LinkedList<TreeNode<Model>> leaves = dbscanResultsTree.getLeaves();
			long biggestClusterSize = getNodeWithBiggestModel(leaves).getData().size();
			if(leaves.size() * biggestClusterSize > model.size() && dbscanResultsTree.getNodesOnLevel(level).size() > 0)
			{
				minPoints = minPoints * 2;
			} else
			{
				biggestUsableClusterFound = true;
			}
		}
		logger.info("Done building DBSCAN tree.");
		
		// Delete small clusters and use their bigger parent clusters instead with smaller minStatements
		logger.info("Start joining small parts.");
		joinSmallParts(dbscanResultsTree, getNodeWithBiggestModel(dbscanResultsTree.getLeaves()).getData().size());
		logger.info("Done joining small parts.");
		
		LinkedList<Model> result = new LinkedList<Model>();
		for(TreeNode<Model> node : dbscanResultsTree.getLeaves())
		{
			result.add(node.getData());
		}
		result = balanceWithFreeStatements(result, model);
		
		logger.info("Done DBSCAN partitioning and balancing.");
		outputPartitioningDetails(result);
		
		return result;
	}
	
	/**
	 * Outputs and logs information about the partitioning.
	 * - Number of parts
	 * - Number of statements in all parts
	 * - Size of smallest part
	 * - Size of biggest part
	 * - Average size of parts
	 * 
	 * @param partitioning
	 */
	private static void outputPartitioningDetails(LinkedList<Model> partitioning)
	{
		Logger logger = Log.getLogger();
		
		long statements = 0;
		long smallest = Long.MAX_VALUE;
		long biggest = 0;
		for(Model m : partitioning)
		{
			statements += m.size();
			if(smallest > m.size())
			{
				smallest = m.size();
			}
			if(biggest < m.size())
			{
				biggest = m.size();
			}
		}
		logger.info("Number of Parts: " + partitioning.size());
		logger.info("Number of Statements: " + statements);
		logger.info("Smalest: " + smallest);
		logger.info("Biggest: " + biggest);
		logger.info("Average: " + statements/partitioning.size());
		
	}
	
	/**
	 * Finds node containing the model with the most statements from a list of TreeNodes containing Models
	 * @param nodes
	 * @return TreeNode containing biggest model
	 */
	private static TreeNode<Model> getNodeWithBiggestModel(LinkedList<TreeNode<Model>> nodes)
	{
		TreeNode<Model> node = nodes.getFirst();
		for(TreeNode<Model> n : nodes)
		{
			if(n.getData().size() > node.getData().size())
			{
				node = n;
			}
		}
		return node;
	}
	
	// If a cluster is smaller then the biggest used cluster (leaf) delete its children.
	// Therefore using a bigger but less strongly connected cluster
	/**
	 * Takes a partitioning tree. Cuts off all subtrees where the root of the subtree is smaller then the biggest leaf of the complete tree.
	 * This root is then a leave.
	 * @param node
	 * @param maxSize
	 */
	private static void joinSmallParts(TreeNode<Model> node, long maxSize)
	{
		if(node.getData().size() <= maxSize)
		{
			node.removeChildren();
		} 
		else if(!node.isLeaf())
		{
			for(TreeNode<Model> child : node.getChildren())
			{
				joinSmallParts(child, maxSize);
			}
		}
	}
	
	/**
	 * Partitions a model using a DBSCAN approach.
	 * Highly connected parts of the model stay in one part.
	 * A subject that is connected to at least minStatements statements starts a part. 
	 * Then the statements of every connected subject that also fulfills this condition gets added to this part.
	 * @param model
	 * @param minStatements
	 * @param silent if true no logger messages are output.
	 * @return partitioning = list of models
	 */
	public static LinkedList<Model> dbscan (Model model, int minStatements, boolean silent)
	{
		Logger logger = Log.getLogger();
		logger.info("Start DBSCAN for model of size: " + model.size());
		
		if(model == null || model.size() == 0)
		{
			return null;
		}
		// Iterator over all Resources in the model
		ResIterator resourceIterator = model.listResourcesWithProperty(null);
		
		// Hashset used to store all Resources that have been checked if they belong to a cluster or not
		HashSet<Resource> checkedResources = new HashSet<Resource>((int) (model.listResourcesWithProperty(null).toList().size() * 1.33));
		
		// List storing all clusters found
		// Clusters are stored as their own model
		LinkedList<Model> clusters = new LinkedList<Model>();
		
		// Go over all Resources and test if they belong to a cluster
		while(resourceIterator.hasNext())
		{
			// Get the next Resource, this will be the starting point for DBSCAN and a potential new cluster
			Resource resource = resourceIterator.next();
			
			// If this Resource has not been checked yet, do so
			if(!checkedResources.contains(resource)) 
			{
				// Store Resource to the checked Resources, after this it will not have to be looked at again
				checkedResources.add(resource);
				
				// Calculate cluster
				Cluster c = new Cluster(model, checkedResources, resource, minStatements);
				
				// If an actual cluster is returned
				if (c.getCluster().size() > 0) 
				{
					// Add cluster to the found clusters
					clusters.add(c.getCluster());
					
					// Add all Resources in the new cluster the the checked Resources (they cannot belong to a second cluster)
					checkedResources.addAll(c.getCluster().listResourcesWithProperty(null).toList());
				}
			}
		}
			
		if(!silent)
		{
			logger.info("Done with DBSCAN.");
			outputPartitioningDetails(clusters);
		}
		return clusters;
	}
	
	/**
	 * Takes a partitioning and the partitioned model. 
	 * Then distributes statements of the model that not yet belong to a part such that the smaller parts get padded.
	 * If enough free statements are available all parts will be of the same size in the end.
	 * @param partitioning
	 * @param fullModel
	 * @return padded partitioning
	 */
	private static LinkedList<Model> balanceWithFreeStatements(LinkedList<Model> partitioning, Model fullModel)
	{
		Logger logger = Log.getLogger();
		logger.info("Start balancing the partitioning by padding little parts with left over/free Statements.");
		
		if(partitioning.isEmpty())
		{
			logger.info("Partitioning is emtpy.");
			return partitioning;
		}
		
		logger.info("Start finding free statements.");
//		Model freeStatementsModel = fullModel;
//		for(Model model : partitioning)
//		{
//			freeStatementsModel = freeStatementsModel.difference(model);
//		}
		Model combinedParts = ModelFactory.createDefaultModel();
		for(Model model : partitioning)
		{
			combinedParts.add(model);
		}
		Model freeStatementsModel = fullModel.difference(combinedParts);
		
		// If there are no free Statements that could be used for padding nothing is to be done here
		if(freeStatementsModel.size() == 0)
		{
			logger.info("Done finding free statements. No free Statements to distribute.");
			return partitioning;
		}
		
		logger.info("Done finding free statements. Number of free Statements to distribute: " + freeStatementsModel.size());
		
		List<Statement> freeStatements = freeStatementsModel.listStatements().toList();
		
		// ------------- JUST A TEST --------------------
//		Model freeStatementsModel2 = fullModel;
//		for(Model model : partitioning)
//		{
//			freeStatementsModel2 = freeStatementsModel2.difference(model);
//		}
//		
//		// If there are no free Statements that could be used for padding nothing is to be done here
//		if(freeStatementsModel2.size() == 0)
//		{
//			logger.info("No free Statements to distribute.");
//			return partitioning;
//		}
//		
//		logger.info("Number of free Statements to distribute: " + freeStatementsModel2.size());
//		
//		List<Statement> freeStatementsList2 = freeStatementsModel2.listStatements().toList();
//		
//		boolean statementListDeterministic = true;
//		for(int i = 0; i < freeStatementsList.size(); i++)
//		{
//			if(!freeStatementsList.get(i).equals(freeStatementsList2.get(i)))
//			{
//				logger.info("List position " + i + " is not matching. THIS STEP IS NOT DETERMINISTC.");
//				statementListDeterministic = false;
//			}
//		}
//		logger.info("Creation of list of free statements is deterministic: " + statementListDeterministic);
		// ------------- END OF TEST --------------------
		Collections.sort(partitioning, new ModelComperator());
		LinkedList<Tuple<Long, Integer>> sizeOccurrence = calculateSizeOccurrenceList(partitioning);
		
		// ------------- JUST A TEST --------------------
//		LinkedList<Tuple<Long, Integer>> sizeOccurrence2 = calculateSizeOccurrenceList(partitioning);
//		boolean sizeOccurrenceDeterministic = true;
//		for(int i = 0; i < sizeOccurrence.size(); i++)
//		{
//			if(!sizeOccurrence.get(i).equals(sizeOccurrence2.get(i)))
//			{
//				logger.info("SizeOccurrence not deterministic at " + i + ".");
//				sizeOccurrenceDeterministic = false;
//			}
//		}
//		logger.info("SizeOccurrence determinstic: " + sizeOccurrenceDeterministic);
		// ------------- END OF TEST --------------------
		
		long targetSize = calculateSizeAfterPadding(sizeOccurrence, freeStatements.size());
		
		// ------------- JUST A TEST --------------------
//		long targetSize2 = calculateSizeAfterPadding(sizeOccurrence, freeStatementsList.size());
//		logger.info("Target size is the same: " + (targetSize == targetSize2));
		// ------------- END OF TEST --------------------
		
		LinkedList<Statement> freeStatementsList = new LinkedList<Statement>(freeStatements);
		logger.info("Start padding parts up to target size with free statements.");
		for(Model model : partitioning)
		{
			if(model.size() < targetSize)
			{
				long nrStatementsToAdd = targetSize - model.size();
				LinkedList<Statement> statementsToAdd = new LinkedList<Statement>();
				while(nrStatementsToAdd > 0)
				{
					statementsToAdd.add(freeStatementsList.get(0));
					freeStatementsList.remove(0);
					nrStatementsToAdd--;
				}
				model.add(statementsToAdd);
			}
		}
		logger.info("Done padding parts up to target size.");
		
		logger.info("Number of free Statements after size padding: " + freeStatementsList.size());
		int i = 0;
		while(!freeStatementsList.isEmpty())
		{
			partitioning.get(i).add(freeStatementsList.getFirst());
			freeStatementsList.removeFirst();
			i++;
		}
		logger.info("Done balancing partitioning with free statements.");
		return partitioning;
	}
	
	/**
	 * Compares Models by their size.
	 * Used to sort them in increasing order.
	 * 
	 * An error may occur in the very rare case that 3 models are of the same size and have the same hashCode. This can result in a circle.
	 * @author ---
	 *
	 */
	static class ModelComperator implements Comparator<Model>
	{
		@Override
		public int compare(Model m1, Model m2)
		{
			if(m1.size() < m2.size())
			{
				return -1;
			} else if(m1.size() > m2.size())
			{
				return 1;
			} else
			{
				if(m1.hashCode() < m2.hashCode())
				{
					return -1;
				} else
				{
					return 1;
				}
			}
		}
	}
	
	/**
	 * For a given list of models computes a list containing the sizes of models and how many models are of this size.
	 * @param List of models
	 * @return LinkedList of Tuples where x is the size of the model and y is how many models are of this size
	 */
	private static LinkedList<Tuple<Long, Integer>> calculateSizeOccurrenceList(LinkedList<Model> models)
	{
		LinkedList<Tuple<Long, Integer>> sizeOccurrence = new LinkedList<Tuple<Long, Integer>>();
		long size = models.getFirst().size();
		int occurrence = 0;
		for(Model model : models)
		{
			if(model.size() == size)
			{
				occurrence++;
			} else
			{
				sizeOccurrence.add(new Tuple<Long, Integer>(size, occurrence));
				size = model.size();
				occurrence = 1;
			}
		}
		// Add the the occurrence of the last and biggest models as well
		sizeOccurrence.add(new Tuple<Long, Integer>(models.getLast().size(), occurrence));
		
		return sizeOccurrence;
	}
	
	/**
	 * Calculates the size to which the parts of a partitioning can be padded with statements that are not yet assigned to a part.
	 * @param sizeOccurrence list of part sizes
	 * @param aviableStatements number of free/not assigned statements
	 * @return size to which the smaller parts can be padded.
	 */
	private static long calculateSizeAfterPadding(LinkedList<Tuple<Long, Integer>> sizeOccurrence, long aviableStatements)
	{
		Logger logger = Log.getLogger();
		
		long size = 0;
		long active = sizeOccurrence.getFirst().y;
		long neededStatements = 0;
		int i = 0;
		while(i < sizeOccurrence.size() - 1)
		{
			long sizeDifference = sizeOccurrence.get(i + 1).x - sizeOccurrence.get(i).x;
			long newNeededStatements = neededStatements + sizeDifference * active;
			if(newNeededStatements > aviableStatements)
			{
				break;
			}
			active += sizeOccurrence.get(i + 1).y;
			neededStatements = newNeededStatements;
			size = sizeOccurrence.get(i + 1).x;
			i++;
		}
		long leftoverStatements = aviableStatements - neededStatements;
		size += Math.floorDiv(leftoverStatements, active);
		logger.info("Minimum size after padding will be: " + size);
		return size;
	}
	
	/**
	 * Takes a model and partitions it. 
	 * All statements where a given resource is either the subject or object, together form one part.
	 * This means there is one part for every resource in the model.
	 * Statements where the subject and object are not the same resource appear twice in two different parts. (Parts are not exclusive).
	 * Therefore the total number of statements is between the number of statements in the model and twice that number.
	 * @param model
	 */
	public static LinkedList<Model> partitionResorces (Model model)
	{
		Logger logger = Log.getLogger();
		logger.info("Start partitioning model into resources.");
		
		if(model.isEmpty() || model == null)
		{
			logger.info("Cannot partition this model as it is empty or null.");
			return null;
		}
		
		LinkedList<Model> partitions = new LinkedList<Model>();
		HashSet<Resource> resources = new HashSet<Resource>((int) model.size());
		List<Resource> subjects = model.listSubjects().toList();
		List<RDFNode> objects = model.listObjects().toList();
		resources.addAll(subjects);
		for(RDFNode object : objects)
		{
			if(object.isResource() && !resources.contains(object))
			{
				resources.add((Resource) object);
			}
		}
		
		for(Resource r : resources)
		{
			List<Statement> statementsWithSubject = model.listStatements(r, null, (RDFNode) null).toList();
			List<Statement> statementsWithObject = model.listStatements(null, null, r).toList();
			LinkedList<Statement> statements = new LinkedList<Statement>(statementsWithSubject);
			statements.addAll(statementsWithObject);
			partitions.add(ModelFactory.createDefaultModel().add(statements));
		}
		
		logger.info("Done resource partitioning model.");
		outputPartitioningDetails(partitions);
		return partitions;
	}
	
	/**
	 * Partitions given model.
	 * Takes a subject and then joins all statements within distance of diameter into one part.
	 * Statements only belongs to one part. All statements belong to a part.
	 * May not be deterministic.
	 * @param model
	 * @param diameter
	 * @return
	 */
	public static LinkedList<Model> partitionNeighborhood (Model model, int diameter)
	{
		Logger logger = Log.getLogger();
		logger.info("Start partitioning model into parts of neighors within " + diameter + " range.");
		
		if(model.isEmpty() || model == null)
		{
			logger.info("Cannot partition this model as it is empty or null.");
			return null;
		}
		
		int k = 10000;
		LinkedList<Model> subModels = new LinkedList<Model>();
		
		// List all Subjects in model
		List<Resource> subjects = model.listSubjects().toList();
		
		HashSet<Resource> visited = new HashSet<Resource>(subjects.size());
		
		// while there is a subject that has not been added to a part, build a new part around said subject
		for(Resource center : subjects)
		{
			// skip rest of the loop body if this subject already has been processed
			if(visited.contains(center))
			{
				continue;
			}
			int step = 0;
			List<Statement> statements = model.listStatements(center, null, (RDFNode) null).toList();
			
			LinkedList<Resource>[] resourcesInDistance = new LinkedList[diameter];
			// Create a list for every step
			for(int i = 0; i < diameter; i++)
			{
				resourcesInDistance[i] = new LinkedList<Resource>();
			}
			
			for(Statement s : statements)
			{
				if(s.getObject().isResource())
				{
					resourcesInDistance[step].add((Resource) s.getObject());
				}
			}
			
			visited.add(center);
			
			while(step < diameter - 1)
			{
				step++;
				for(Resource r : resourcesInDistance[step - 1])
				{
					if(!visited.contains(r))
					{
						List<Statement> newStatements = model.listStatements(r, null, (RDFNode) null).toList();
						if(newStatements != null)
						{
							statements.addAll(newStatements);
							
							for(Statement s : newStatements)
							{
								if(s.getObject().isResource())
								{
									resourcesInDistance[step].add((Resource) s.getObject());
								}
							}
						}
						visited.add(r);
					}
				}
			}
			subModels.add(ModelFactory.createDefaultModel().add(statements));
			
			if(visited.size() > k)
			{
				logger.info("Subjects processed: " + visited.size() + " Out of: " + subjects.size());
				k = k + 10000;
			}
		}
		
		logger.info("Done range-partitioning model.");
		outputPartitioningDetails(subModels);
		return subModels;
	}
}
