package communication;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import protocol.Log;
import protocol.Tuple;
 
/**
 * Class used for communication
 * This Server waits for a Client to connect and can send and receive objects
 * 
 * @author ---
 *
 */
 public class Server
 {
	 private static Server instance;
	 private ServerSocket serverSocket;
	 private Socket client;
	 private CountingOutputStream countingOut; // MEASUREMENT
	 private CountingInputStream countingIn;
	 
	 /**
	  * Creates a Server that uses a SSL connection to communicate with a Client
	  * Singleton
	  */
	 private Server() 
	 {
		 Logger logger = Log.getLogger();
		 int port = 11111;
		 try 
		 {
			 SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			 serverSocket = ssf.createServerSocket(port);
			 client = waitForRegistration();
			 countingOut = new CountingOutputStream(client.getOutputStream());  // MEASURING
			 countingIn = new CountingInputStream(client.getInputStream());  // MEASURING
		 } catch (IOException e) 
		 {
			 logger.info("Exception in communication, when trying to create server. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
			 System.exit(1);
		 }
	 }
 
	 /**
	  * 
	  * @return Client
	  */
	 public static Server getServer() 
	 {
		 if(instance == null)
		 {
			 instance = new Server();
		 }
		 return instance;
	 }
	

	 /**
	  * Waits until a Client registrates 
	  * @return Socket to the Client
	  * @throws IOException
	  */
	 private Socket waitForRegistration() throws IOException
	 {
		 Socket socket = serverSocket.accept(); // blocks until a client registrated
		 return socket;
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
//	    return ReadWrite.readModel(socket);
    	return ReadWrite.readModel(countingIn);
    }
	 
	 /**
	 * Receives any serializable object
	 * @return object send by the client.
	 */
	 public <T> T readObject()
	 {
//		 return ReadWrite.<T>readObject(client);
		 return ReadWrite.<T>readObject(countingIn);
	 }
	 
	 /**
     * Can send any serializable object
     * @param object object to be sent, needs to be serializable
     */
	 public <T> void sendObject(T object)
	 {
//		 ReadWrite.sendObject(client, object);
		 ReadWrite.sendObject(countingOut, object); // MEASUREMENT
	 }
	 
	// ------------- ONLY NEEDED FOR INSECURE / NO PRIVACY COMPARISON ----------------
	 /**
	  * Sends a HashMap<Tuple<Resource, RDFNode>,Integer>
	  * @param hashmap HashMap<Tuple<Resource, RDFNode>,Integer>
	  */
	 public void sendHashMap(HashMap<Tuple<Resource, RDFNode>,Integer> hashmap)
	 {
//		 ReadWrite.sendHashMap(client, hashmap);
		 ReadWrite.sendHashMap(countingOut, hashmap);
	 }
	 
	 /**
	  * Sends a HashMap<RDFNode, Integer>
	  * @param hashmap HashMap<RDFNode, Integer>
	  */
	 public void sendHashMap2(HashMap<RDFNode, Integer> hashmap)
	 {
//		 ReadWrite.sendHashMap2(client, hashmap);
		 ReadWrite.sendHashMap2(countingOut, hashmap);
	 }
	 
	 /**
	  * Sends a HashMap<Resource, Integer>
	  * @param hashmap HashMap<Resource, Integer>
	  */
	 public void sendHashMap3(HashMap<Resource, Integer> hashmap)
	 {
//		 ReadWrite.sendHashMap3(client, hashmap);
		 ReadWrite.sendHashMap3(countingOut, hashmap);
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