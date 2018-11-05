package knowledgegraphpartitioning;

import java.util.HashSet;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

public class Cluster
{
	
	// Contains the cluster that currently is built
	Model clusterModel = ModelFactory.createDefaultModel();
	
	/**
	 * Used by DBSCAN partitioning. For a given starting point (subject) finds the surrounding cluster.
	 * @param model
	 * @param checkedResources
	 * @param startingPoint
	 * @param minStatements
	 */
	public Cluster (Model model, HashSet<Resource> checkedResources, Resource startingPoint, int minStatements) 
	{
		findCluster(model, checkedResources, startingPoint, minStatements);
	}
	
	// Takes a model,
	// 			Resources that already have been checked,
	//			a starting point (Resource)
	//			and a minimum number of Statements
	// Calculates a cluster from the starting point
	// Uses DBSCAN like algorithm
	/**
	 * For a model and a subject as starting point finds the highly connected part around it if it belongs to one.
	 * @param model
	 * @param checkedResources
	 * @param startingPoint
	 * @param minStatements
	 */
	private void findCluster(Model model, HashSet<Resource> checkedResources, Resource startingPoint, int minStatements)
	{
		// Find all statements with this resource (startingPoint) as Subject or Object
		List<Statement> statementsWithSubject = model.listStatements(startingPoint, null, (RDFNode) null).toList();
			
		// Check if number of statements passes threshold, therefore resource is a core and all statements belong to the subgraph
		if(statementsWithSubject.size() >= minStatements)
		{
			// Add statements to cluster
			clusterModel.add(statementsWithSubject);
		}
		
		boolean newCandidatesBool = true;
		
		// As long as new Resources keep beeing added to the cluster
		while(newCandidatesBool)
		{
			// Find all Resources in the model of the cluster
			// They are potential core point, the cluster could potentially grow from those points
			List<Resource> candidates = clusterModel.listResourcesWithProperty(null).toList();
			// Store the number of Resources in the cluster model before the upcoming iteration
			// Used to see whether the cluster has grown or everything has been checked
			int clusterSize = candidates.size();
			// Set flagg to false
			newCandidatesBool = false;
			
			// Go all Resources in the cluster model and check if the cluster can be extended from there
			for(Resource candidate : candidates)
			{
				// If this Resource already has been checked, skip it
				if(!checkedResources.contains(candidate))
				{
					// Add Resource to checked Resources
					checkedResources.add(candidate);
					// Find all statements with this resource as Subject or Object
					List<Statement> candidateStatementsWithSubject = model.listStatements(candidate, null, (RDFNode) null).toList();
					
					// If the Resource appears in at least the threshold of minStatements 
					if(candidateStatementsWithSubject.size() >= minStatements)
					{
						// Add statements to cluster
						clusterModel.add(candidateStatementsWithSubject);
					}
				}
			}
			
			// Update candidate list to all Resources contained in the cluster model, now possibly containing new Resources after the last iteration
			candidates = clusterModel.listResourcesWithProperty(null).toList();

			// Check if there are more Resources in the cluster model then before, therefore there would be new candidates
			if(clusterSize < candidates.size())
			{
				newCandidatesBool = true;
			}
		}
	}
	
	/**
	 * Getter 
	 * @return model which represents this cluster
	 */
	public Model getCluster()
	{
		return clusterModel;
	}
}