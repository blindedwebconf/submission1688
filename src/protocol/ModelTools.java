package protocol;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

/**
 * Class containing useful tools for Jena models.
 * @author ---
 *
 */
public class ModelTools 
{
	/**
	 * Writes a Model to a file in turtle format.
	 * @param model
	 * @param path
	 */
	public static void writeModelToFile(Model model, String path)
	{
		FileOutputStream out;
		try 
		{
			out = new FileOutputStream(path);
			RDFDataMgr.write(out, model, RDFFormat.TTL);
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes a list of models to files.
	 * @param models
	 */
	public static void writeModelsToFile(LinkedList<Model> models, String folder)
	{
		Integer number = 0;
		for(Model model : models)
		{
			writeModelToFile(model, folder.concat("Model" + number.toString() + ".ttl"));
			number++;
		}
	}
	
	/**
	 * Encodes a model as a String in RDF/XML-ABBREV format
	 * @param model
	 * @return string encoding model
	 */
	public static String modelToString(Model model)
	{
		String result = "";								// String the model will be represented in
		String syntax = "RDF/XML-ABBREV"; 				// Define syntax with which the model will be encoded. also try "N-TRIPLE" and "TURTLE"
		StringWriter out = new StringWriter();
		model.write(out, syntax);
		result = out.toString();						// Turn into String
		result = "<!--This is a model-->\n" + result;	// This line is added as a xml comment and is used to recognize a properly decrypted model later on (BuyerObtainKGPart.stringToModel)
		
		return result;
	}
	
	/**
	 * Turns a string encoding a model back into a model.
	 * The first line of the string needs to be "<!--This is a model-->"
	 * @param string
	 * @return model
	 */
	public static Model stringToModel(String string)
	{
		Logger logger = Log.getLogger();
		
		// Get first line of the string and check if for line marking that the decryption worked
		String[] lines = string.split("\n");
		// Check if the first line matches, if so turn String into model
		if(lines[0].equals("<!--This is a model-->"))
		{
			// Create a new empty Model
			Model modelFromString = ModelFactory.createDefaultModel();
			try 
			{
				// Try to read in a model
				modelFromString.read(IOUtils.toInputStream(string,"UTF-8"), null);
			} catch (IOException e) {
				logger.info("Exception when trying to turn a string into a model. \n" 
						+ e.getMessage() + "\n"
						+ e.toString() + "\n"
						+ "Protocol is being terminated.");
			 System.exit(1);
			}
			return modelFromString;
		}
		return null;
	}
}
