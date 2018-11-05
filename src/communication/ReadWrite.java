package communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;


import dataquality.BuyerObtainKGPart;
import dataquality.SellerPrepareKG;
import nosecuritycomparison.SerializedRDFNode;
import protocol.Log;
import protocol.ModelTools;
import protocol.Tuple;

/**
 * Contains sending and receiving methods to be used by both the server and client
 * 
 * @author ---
 *
 */
public class ReadWrite 
{
	/**
     * Can send any serializable object
     * terminates program on IOException
     *  
     * @param socket the objects should be sent to
     * @param object object to be sent, needs to be serializable
     */
//	public static <T> void sendObject(Socket socket, T object)
	public static <T> void sendObject(CountingOutputStream count, T object)
	{
		Logger logger = Log.getLogger();
		
		 try 
		 {
			 ObjectOutputStream os = new ObjectOutputStream(count);
			 os.writeObject(object);
		 } catch (IOException e) 
		 {
			 logger.info("Exception in communication, when trying to send a serializable object. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
			 System.exit(1);
		 }
	}
	
	/**
	 * Receives any serializable object
	 * 
	 * terminates program on IOException
	 *  
	 * @param socket to listen to
	 * @return object which has been received.
	 */
//	public static <T> T readObject(Socket socket)
	public static <T> T readObject(CountingInputStream count)
	{
		Logger logger = Log.getLogger();
		
		T received = null;
		try 
		{
//			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			ObjectInputStream is = new ObjectInputStream(count);
			received = (T) is.readObject();
		} catch (IOException | ClassNotFoundException e) 
		{
			logger.info("Exception in communication, when trying to read a serializable object. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
			System.exit(1);
		}
		return received;
	}
	
	/**
	 * Sends a model
	 * first encodes the model as a string which is serializable to be sent
	 * 
	 * terminates program on IOException
	 * 
	 * @param socket the model should be sent to
	 * @param model to be sent
	 */
//	public static void sendModel(Socket socket, Model model)
	public static void sendModel(CountingOutputStream count, Model model)
	{
		Logger logger = Log.getLogger();
		
		String modelAsString = ModelTools.modelToString(model);
		try 
		{
//			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			ObjectOutputStream os = new ObjectOutputStream(count);
			os.writeObject(modelAsString);
		} catch (IOException e) 
		{
			logger.info("Exception in communication, when trying to send a model. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
    	
	}
	
	/**
	 * Receives a model
	 * Received model is encoded as a String
	 * Turns it back into a model
	 * 
	 * terminates program on IOException
	 *  
	 * @param socket to listen to
	 * @return model
	 */
//	public static Model readModel(Socket socket)
	public static Model readModel(CountingInputStream count)
	{
		Logger logger = Log.getLogger();
		
		String modelAsString = null;
		try 
		{
//			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			ObjectInputStream is = new ObjectInputStream(count);
			modelAsString = (String) is.readObject();
		} catch (IOException | ClassNotFoundException e) 
		{
			logger.info("Exception in communication, when trying to read a model. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
		Model model = ModelTools.stringToModel(modelAsString);
		return model;
	}
	
	
	// ------------ ONLY USED FOR INSECURE/NO PRIVACY COMPARISON ----------------------------
	/**
	 * Serializes and sends a HashMap<Tuple<Resource, RDFNode>,Integer>
	 * 
	 * terminates program on IOException
	 * 
	 * @param socket to send to
	 * @param hashmap HashMap<Tuple<Resource, RDFNode>,Integer>
	 */
//	public static void sendHashMap(Socket socket, HashMap<Tuple<Resource, RDFNode>,Integer> hashmap)
	public static void sendHashMap(CountingOutputStream count, HashMap<Tuple<Resource, RDFNode>,Integer> hashmap)
	{
		Logger logger = Log.getLogger();
		
		HashMap<Tuple<String, SerializedRDFNode>,Integer> serializedHashMap = new HashMap<Tuple<String, SerializedRDFNode>,Integer>();
		for(Entry<Tuple<Resource, RDFNode>, Integer> entry : hashmap.entrySet())
		{
			String resourceString = entry.getKey().x.toString();
			SerializedRDFNode sNode = new SerializedRDFNode(entry.getKey().y);
			serializedHashMap.put(new Tuple<String, SerializedRDFNode>(resourceString, sNode), entry.getValue());
		}
		// send serialized version
		try 
		{
//			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			ObjectOutputStream os = new ObjectOutputStream(count);
			os.writeObject(serializedHashMap);
		} catch (IOException e) 
		{
			logger.info("Exception in communication, when trying to send a hashmap. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
    	
	}
	
	/**
	 * Receives a HashMap<Tuple<Resource, RDFNode>, Integer>
	 * Undoes serialisation encoding
	 * 
	 * terminates program on IOException
	 * 
	 * @param socket to listen to
	 * @return HashMap<Tuple<Resource, RDFNode>, Integer>
	 */
//	public static HashMap<Tuple<Resource, RDFNode>,Integer> readHashMap(Socket socket)
	public static HashMap<Tuple<Resource, RDFNode>,Integer> readHashMap(CountingInputStream count)
	{
		Logger logger = Log.getLogger();
		
		HashMap<Tuple<String, SerializedRDFNode>,Integer> serializedHashMap = null;
		try 
		{
//			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			ObjectInputStream is = new ObjectInputStream(count);
			serializedHashMap = (HashMap<Tuple<String, SerializedRDFNode>,Integer>) is.readObject();
		} catch (IOException | ClassNotFoundException e) 
		{
			logger.info("Exception in communication, when trying to read a hashmap. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
		
		HashMap<Tuple<Resource, RDFNode>,Integer> hashmap = new HashMap<Tuple<Resource, RDFNode>,Integer>();
		for(Entry<Tuple<String, SerializedRDFNode>, Integer> entry : serializedHashMap.entrySet())
		{
			Resource r = ResourceFactory.createResource(entry.getKey().x);
//			Resource r = entry.getKey().x.getResource();
			String nodeString = entry.getKey().y.getRDFNodeString();
			RDFNode node = null;
			if(entry.getKey().y.isResource())
			{
				node = (RDFNode) ResourceFactory.createResource(nodeString);
			} else
			{
				node = (RDFNode) ResourceFactory.createStringLiteral(nodeString);
			}
			hashmap.put(new Tuple<Resource, RDFNode>(r, node), entry.getValue());
		}
		
		return hashmap;
	}
	
	/**
	 * Serializes and sends a HashMap<RDFNode,Integer>
	 * 
	 * terminates program on IOException
	 * 
	 * @param socket to send to
	 * @param hashmap HashMap<RDFNode,Integer>
	 */
//	public static void sendHashMap2(Socket socket, HashMap<RDFNode,Integer> hashmap)
	public static void sendHashMap2(CountingOutputStream count, HashMap<RDFNode,Integer> hashmap)
	{
		Logger logger = Log.getLogger();
		
		HashMap<SerializedRDFNode,Integer> serializedHashMap = new HashMap<SerializedRDFNode,Integer>();
		for(Entry<RDFNode, Integer> entry : hashmap.entrySet())
		{
			SerializedRDFNode sNode = new SerializedRDFNode(entry.getKey());
			serializedHashMap.put(sNode, entry.getValue());
		}
		
		try {
//			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			ObjectOutputStream os = new ObjectOutputStream(count);
			os.writeObject(serializedHashMap);
		} catch (IOException e) {
			logger.info("Exception in communication, when trying to send a hashmap2. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
    	
	}
	
	/**
	 * Receives a HashMap<RDFNode, Integer>
	 * Undoes serialisation encoding
	 * 
	 * terminates program on IOException
	 * 
	 * @param socket to listen to
	 * @return HashMap<RDFNode, Integer>
	 */
//	public static HashMap<RDFNode,Integer> readHashMap2(Socket socket)
	public static HashMap<RDFNode,Integer> readHashMap2(CountingInputStream count)
	{
		Logger logger = Log.getLogger();
		
		HashMap<SerializedRDFNode,Integer> serializedHashMap = null;
		try {
//			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			ObjectInputStream is = new ObjectInputStream(count);
			serializedHashMap = (HashMap<SerializedRDFNode,Integer>) is.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.info("Exception in communication, when trying to read a hashmap2. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
		
		HashMap<RDFNode,Integer> hashmap = new HashMap<RDFNode,Integer>();
		for(Entry<SerializedRDFNode, Integer> entry : serializedHashMap.entrySet())
		{
			String nodeString = entry.getKey().getRDFNodeString();
			RDFNode node = null;
			if(entry.getKey().isResource())
			{
				node = (RDFNode) ResourceFactory.createResource(nodeString);
			} else
			{
				node = (RDFNode) ResourceFactory.createStringLiteral(nodeString);
			}
			hashmap.put(node, entry.getValue());
		}
		
		return hashmap;
	}
	
	/**
	 * Serializes and sends a HashMap<Resource,Integer>
	 * 
	 * terminates program on IOException
	 * 
	 * @param socket to send to
	 * @param hashmap HashMap<Resource,Integer>
	 */
//	public static void sendHashMap3(Socket socket, HashMap<Resource,Integer> hashmap)
	public static void sendHashMap3(CountingOutputStream count, HashMap<Resource,Integer> hashmap)
	{
		Logger logger = Log.getLogger();
		
		HashMap<String,Integer> serializedHashMap = new HashMap<String, Integer>();
		for(Entry<Resource, Integer> entry : hashmap.entrySet())
		{
			String resourceString = entry.getKey().toString();
			serializedHashMap.put(resourceString, entry.getValue());
		}
		
		try {
//			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			ObjectOutputStream os = new ObjectOutputStream(count);
			os.writeObject(serializedHashMap);
		} catch (IOException e) {
			logger.info("Exception in communication, when trying to send a hashmap3. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
	}
	
	/**
	 * Receives a HashMap<Resource, Integer>
	 * Undoes serialisation encoding
	 * 
	 * terminates program on IOException
	 * 
	 * @param socket to listen to
	 * @return HashMap<Resource, Integer>
	 */
//	public static HashMap<Resource, Integer> readHashMap3(Socket socket)
	public static HashMap<Resource, Integer> readHashMap3(CountingInputStream count)
	{
		Logger logger = Log.getLogger();
		
		HashMap<String, Integer> serializedHashMap = null;
		try {
//			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			ObjectInputStream is = new ObjectInputStream(count);
			serializedHashMap = (HashMap<String, Integer>) is.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.info("Exception in communication, when trying to read a hashmap3. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
		}
		
		HashMap<Resource, Integer> hashmap = new HashMap<Resource, Integer>();
		for(Entry<String, Integer> entry : serializedHashMap.entrySet())
		{
			Resource r = ResourceFactory.createResource(entry.getKey());
			hashmap.put(r, entry.getValue());
		}
		
		return hashmap;
	}
}
