package communication;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import protocol.GetUserInput;
import protocol.Log;
import protocol.Tuple;

/**
 * Class used for communication
 * This Client connects to Server class and can send and receive objects
 * 
 * @author ---
 *
 */
public class Client
{
	private static Client instance;
	private Socket socket;
	private CountingOutputStream countingOut; // MEASUREMENT
	private CountingInputStream countingIn; // MEASUREMENT
	
	/**
	 * Creates a client that establishes a SSL connection to the server
	 * Singleton
	 */
	private Client() 
	{
		Logger logger = Log.getLogger();
		String ip = GetUserInput.askIP();
		int port = 11111;
		try 
		{
			SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		    socket = ssf.createSocket(ip, port);
		    countingOut = new CountingOutputStream(socket.getOutputStream());  // MEASURING
		    countingIn = new CountingInputStream(socket.getInputStream());  // MEASURING
		} catch (IOException e) 
		{
			 logger.info("Exception in communication, when trying to create client. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
			 System.exit(1);
		}
	}
	
	/**
	 * 
	 * @return client
	 */
	public static Client getClient() 
	{
		if(instance == null)
		{
			instance = new Client();
		}
		return instance;
	}
	
	 /**
	  * Sends a model
	  * @param model which is supposed to be sent
	  */
	 public void sendModel(Model model)
	 {
//		 ReadWrite.sendModel(client, model);
		 ReadWrite.sendModel(countingOut, model);
	 }
	 
	/**
	 * receives a model
	 * @return model 
	 */
    public Model readModel()
    {
//    	return ReadWrite.readModel(socket);
    	return ReadWrite.readModel(countingIn);
    }
	
    /**
     * Can send any serializable object
     * @param object object to be sent, needs to be serializable
     */
	public <T> void sendObject(T object)
	{
//		ReadWrite.sendObject(socket, object);
		ReadWrite.sendObject(countingOut, object);
	}
	
	/**
	 * Receives any serializable object
	 * @return object send by the server.
	 */
	public <T> T readObject()
    {
//    	return ReadWrite.<T>readObject(socket);
		return ReadWrite.<T>readObject(countingIn);
    }
	
	// ------------- ONLY NEEDED FOR INSECURE / NO PRIVACY COMPARISON ----------------
	/**
	 * receives a HashMap with key: Tuple<Resource, RDFNode> value: Integer
	 * @return HashMap<Tuple<Resource, RDFNode>, Integer>
	 */
	public HashMap<Tuple<Resource, RDFNode>,Integer> readHashMap()
	{
//		return ReadWrite.readHashMap(socket);
		return ReadWrite.readHashMap(countingIn);
	}
	
	/**
	 * receives a HashMap with key: RDFNode value: Integer
	 * @return HashMap<RDFNode, Integer>
	 */
	public HashMap<RDFNode,Integer> readHashMap2()
	{
//		return ReadWrite.readHashMap2(socket);
		return ReadWrite.readHashMap2(countingIn);
	}
	
	/**
	 * receives a HashMap with key: Resource value: Integer
	 * @return HashMap<Resource, Integer>
	 */
	public HashMap<Resource,Integer> readHashMap3()
	{
//		return ReadWrite.readHashMap3(socket);
		return ReadWrite.readHashMap3(countingIn);
	}
	
	// ----------- ONLY NEEDED FOR COMMUNICATION MEASUREMENT -------------------------
	 
	 public long getOutputStreamCount()
	 {
		 return countingOut.getByteCount();
	 }
	 
	 public long getInputStreamCount()
	 {
		 return countingIn.getByteCount();
	 }
}
