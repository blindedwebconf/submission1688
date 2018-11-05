package nosecuritycomparison;

import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

import protocol.Log;

/**
 * Used to serialize a RDFNode
 * @author ---
 *
 */
public class SerializedRDFNode implements Serializable
{
	private static final long serialVersionUID = 1;
	
	private String nodeString;
	private boolean isResource;
	
	/**
	 * Turns a RDFNode into a serializale object.
	 * @param rdfNode to be serialized
	 */
	public SerializedRDFNode(RDFNode rdfNode)
	{
		Logger logger = Log.getLogger();
		
		if(rdfNode.isResource())
		{
			nodeString = rdfNode.toString();
			isResource = true;
		} else
		{
			if(rdfNode.isLiteral())
			{
				Literal literal = (Literal) rdfNode;
				nodeString = literal.getString();
				isResource = false;
			} else
			{
				logger.info("Found neither Resource nor Literal.");
			}
		}
	}
	
	/**
	 * 
	 * @return string encoding RDFNode
	 */
	public String getRDFNodeString()
	{
		return nodeString;
	}
	
	/**
	 * 
	 * @return true if RDFNode is resource, false otherwise
	 */
	public boolean isResource()
	{
		return isResource;
	}
}
