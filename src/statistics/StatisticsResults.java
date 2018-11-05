package statistics;

import java.io.Serializable;

/**
 * Class to store results calculated in Statistics
 * @author ---
 *
 */
public class StatisticsResults implements Serializable
{
	private static final long serialVersionUID = 1;
	
	private long size;					// size of model
	private long resources;				// number different resources in model
	private long subjects;				// number different subjects in model
	private long predicates;			// number different predicates in model
	private long objects;				// number different objects in model
	private long objectResources;		// number different objects resources in model
	private long literals;				// number different literals in model
	private long totalObjectResources;	// total number of object resources in model
	private long totalLiterals;			// total number of literals in model
	private double avgOutgoingLinks;	// average number of outgoing links per subject
	private long minOutgoingLinks;		// minimum number of outgoing links per subject
	private long maxOutgoingLinks;		// maximum number of outgoing links per subject
	private long outgoingLinks25;		// 25% quantile of outgoing links per subject
	private long outgoingLinks50;		// 50% quantile of outgoing links per subject
	private long outgoingLinks75;		// 75% quantile of outgoing links per subject
	private double avgIncomingLinks;	// average number of incoming links per object resource
	private long minIncomingLinks;		// minimum number of incoming links per object resource
	private long maxIncomingLinks;		// maximum number of incoming links per object resource
	private long incomingLinks25;		// 25% quantile of incoming links per object resource
	private long incomingLinks50;		// 50% quantile of incoming links per object resource
	private long incomingLinks75;		// 75% quantile of incoming links per object resource
	private double avgLiterals;			// average number of literals per subject
	private long minLiterals;			// minimum number of literals per subject
	private long maxLiterals;			// maximum number of literals per subject
	private long literals25;			// 25% quantile of literals per subject
	private long literals50;			// 50% quantile of literals per subject
	private long literals75;			// 75% quantile of literals per subject
	private double avgObjectResources;	// average number of object resources per subject
	private long minResObjects;			// minimum number of literals per subject
	private long maxResObjects;			// maximum number of literals per subject
	private long resObjects25;			// 25% quantile of literals per subject
	private long resObjects50;			// 50% quantile of literals per subject
	private long resObjects75;			// 75% quantile of literals per subject
	
	public StatisticsResults()
	{
		
	}

	public long getSize() 
	{
		return size;
	}

	public void setSize(long size) 
	{
		this.size = size;
	}

	public long getResources() 
	{
		return resources;
	}

	public void setResources(long resources) 
	{
		this.resources = resources;
	}

	public long getSubjects() 
	{
		return subjects;
	}

	public void setSubjects(long subjects) 
	{
		this.subjects = subjects;
	}
	
	public long getPredicates() 
	{
		return predicates;
	}

	public void setPredicates(long predicates) 
	{
		this.predicates = predicates;
	}

	public long getObjects() 
	{
		return objects;
	}

	public void setObjects(long objects) 
	{
		this.objects = objects;
	}

	public long getObjectResources() 
	{
		return objectResources;
	}

	public void setObjectResources(long objectResources) 
	{
		this.objectResources = objectResources;
	}

	public long getLiterals() 
	{
		return literals;
	}

	public void setLiterals(long literals) 
	{
		this.literals = literals;
	}

	public long getTotalObjectResources() 
	{
		return totalObjectResources;
	}

	public void setTotalObjectResources(long totalObjectResources) 
	{
		this.totalObjectResources = totalObjectResources;
	}

	public long getTotalLiterals() 
	{
		return totalLiterals;
	}

	public void setTotalLiterals(long totalLiterals) 
	{
		this.totalLiterals = totalLiterals;
	}

	public double getAvgOutgoingLinks() 
	{
		return avgOutgoingLinks;
	}

	public void setAvgOutgoingLinks(double avgOutgoingLinks) 
	{
		this.avgOutgoingLinks = avgOutgoingLinks;
	}

	public double getAvgIncomingLinks() 
	{
		return avgIncomingLinks;
	}

	public void setAvgIncomingLinks(double avgIncomingLinks) 
	{
		this.avgIncomingLinks = avgIncomingLinks;
	}

	public double getAvgLiterals() 
	{
		return avgLiterals;
	}

	public void setAvgLiterals(double avgLiterals) 
	{
		this.avgLiterals = avgLiterals;
	}

	public double getAvgObjectResources() 
	{
		return avgObjectResources;
	}

	public void setAvgObjectResources(double avgObjectResources) 
	{
		this.avgObjectResources = avgObjectResources;
	}

	public long getMinOutgoingLinks() 
	{
		return minOutgoingLinks;
	}

	public void setMinOutgoingLinks(long minOutgoingLinks) 
	{
		this.minOutgoingLinks = minOutgoingLinks;
	}

	public long getMaxOutgoingLinks() 
	{
		return maxOutgoingLinks;
	}

	public void setMaxOutgoingLinks(long maxOutgoingLinks) 
	{
		this.maxOutgoingLinks = maxOutgoingLinks;
	}

	public long getOutgoingLinks25() 
	{
		return outgoingLinks25;
	}

	public void setOutgoingLinks25(long outgoingLinks25) 
	{
		this.outgoingLinks25 = outgoingLinks25;
	}

	public long getOutgoingLinks50() 
	{
		return outgoingLinks50;
	}

	public void setOutgoingLinks50(long outgoingLinks50) 
	{
		this.outgoingLinks50 = outgoingLinks50;
	}

	public long getOutgoingLinks75() 
	{
		return outgoingLinks75;
	}

	public void setOutgoingLinks75(long outgoingLinks75) 
	{
		this.outgoingLinks75 = outgoingLinks75;
	}

	public long getMinLiterals() 
	{
		return minLiterals;
	}

	public void setMinLiterals(long minLiterals) 
	{
		this.minLiterals = minLiterals;
	}

	public long getMaxLiterals() {
		return maxLiterals;
	}

	public void setMaxLiterals(long maxLiterals) 
	{
		this.maxLiterals = maxLiterals;
	}

	public long getLiterals25() 
	{
		return literals25;
	}

	public void setLiterals25(long literals25) 
	{
		this.literals25 = literals25;
	}

	public long getLiterals50() 
	{
		return literals50;
	}

	public void setLiterals50(long literals50) 
	{
		this.literals50 = literals50;
	}

	public long getLiterals75() 
	{
		return literals75;
	}

	public void setLiterals75(long literals75) 
	{
		this.literals75 = literals75;
	}

	public long getMinResObjects() 
	{
		return minResObjects;
	}

	public void setMinResObjects(long minResObjects) 
	{
		this.minResObjects = minResObjects;
	}

	public long getMaxResObjects() 
	{
		return maxResObjects;
	}

	public void setMaxResObjects(long maxResObjects) 
	{
		this.maxResObjects = maxResObjects;
	}

	public long getResObjects25() 
	{
		return resObjects25;
	}

	public void setResObjects25(long resObjects25) 
	{
		this.resObjects25 = resObjects25;
	}

	public long getResObjects50() 
	{
		return resObjects50;
	}

	public void setResObjects50(long resObjects50) 
	{
		this.resObjects50 = resObjects50;
	}

	public long getResObjects75() 
	{
		return resObjects75;
	}

	public void setResObjects75(long resObjects75) 
	{
		this.resObjects75 = resObjects75;
	}

	public long getMinIncomingLinks() 
	{
		return minIncomingLinks;
	}

	public void setMinIncomingLinks(long minIncomingLinks)
	{
		this.minIncomingLinks = minIncomingLinks;
	}

	public long getMaxIncomingLinks() 
	{
		return maxIncomingLinks;
	}

	public void setMaxIncomingLinks(long maxIncomingLinks) 
	{
		this.maxIncomingLinks = maxIncomingLinks;
	}

	public long getIncomingLinks25() 
	{
		return incomingLinks25;
	}

	public void setIncomingLinks25(long incomingLinks25)
	{
		this.incomingLinks25 = incomingLinks25;
	}

	public long getIncomingLinks50() 
	{
		return incomingLinks50;
	}

	public void setIncomingLinks50(long incomingLinks50) 
	{
		this.incomingLinks50 = incomingLinks50;
	}

	public long getIncomingLinks75() 
	{
		return incomingLinks75;
	}

	public void setIncomingLinks75(long incomingLinks75) 
	{
		this.incomingLinks75 = incomingLinks75;
	}
}
