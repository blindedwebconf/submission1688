package protocol;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

/**
 * Class containing methods to read different types of RDF data-sets into a Jena model.
 * @author ---
 *
 */
public class LoadRDF 
{

	/**
	 * Reads in a data-set into a Jena model.
	 * Reads a indexing for the data-set for faster loading.
	 * If this indexing does not exist yet it will be created.
	 * @param modelPath to data-set
	 * @param indexFolderPath to indexing of data-set
	 * @return model of knowledge graph in file
	 */
	public static Model readModel(String modelPath, String indexFolderPath)
	{
		Dataset dataset = TDBFactory.createDataset(indexFolderPath);
		Model model = dataset.getDefaultModel();
		if(model.size() == 0) {
			dataset.begin(ReadWrite.WRITE);
			TDBLoader.loadModel(model, modelPath);
			dataset.commit();
			dataset.end();
		}
		
		Logger logger = Log.getLogger();
		logger.info("Reading done.");
		logger.info("Size of the complete model: " + model.size());
		
		return model;
	}
	
	/**
	 * Reads in a HDT file
	 * @param path to file
	 * @return model of knowledge graph in file
	 */
	public static Model readHDT(String path)
	{
		Logger logger = Log.getLogger();
		
		// Load HDT file using the hdt-java library
		HDT hdt = null;
		try
		{
			hdt = HDTManager.mapIndexedHDT(path, null);
		} catch (IOException e)
		{
			e.printStackTrace();
			logger.info("Exception when reading the knowledge graph.");
			logger.info(e.getMessage());
			logger.info(e.toString());
			System.exit(1);
		}
		 
		// Create Jena Model on top of HDT.
		HDTGraph graph = new HDTGraph(hdt);
		Model model = ModelFactory.createModelForGraph(graph);
		
		logger.info("Reading done.");
		logger.info("Size of the complete model: " + model.size());
		
		return model;
	}
}
