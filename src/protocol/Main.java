package protocol;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import additionalstuff.ModelProcessing;
import nosecuritycomparison.InsecureBuyer;
import nosecuritycomparison.InsecureSeller;

import java.util.*;
import java.util.logging.Logger;


/**
 * This is a two party protocol. It privately compares two knowledge graphs of different parties as to how much information could be gained from a merge.
 * It is run as a two party protocol, where one is the seller and one is the buyer.
 * The first step is to find the intersection between both RDFs. This is done by private set intersection using blind signatures and bloom filters.
 * The second step is to calculate entropy changes between the buyers RDF and the combined RDF, as well as some statistics on the sellers RDF.
 * The third step is to give the buyer parts of the sellers RDF via oblivious transfer.
 * Finally the buyer does some verification that the seller did not cheat.
 * 
 * @author ---
 * 
 */
public class Main 
{
	/**
	 * Main method of protocol.
	 * Gets a bunch of user inputs such as whether it is to be run as buyer or seller and the path to his rdf.
	 * Then calls the corresponding method depending on whether the instance is run as buyer or seller.
	 * @param args
	 */
	public static void main(String[] args) 
	{
		
		boolean runAllSteps = false;
		if(Arrays.stream(args).anyMatch(x -> x.equals("complete")))
		{
			runAllSteps = true;
		}
		GetUserInput.setRunAllSteps(runAllSteps);
		
		// Only use smaller sub-model
		int modelsize = 1;
		for(String s : args)
		{
			if(s.matches("-?\\d+")) 
			{
				modelsize = Integer.parseInt(s);
			}
		}
		
		// Turn of Jena logger
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		
		Scanner scanner = new Scanner(System.in);
		GetUserInput.setScanner(scanner);
		System.out.println("To run the protocol as Seller enter 'seller'. To run the protocol as Buyer enter 'buyer'.");
		String runningAs = scanner.nextLine();
		
		System.out.println("Enter path to Log File.");
		String logPath = scanner.nextLine();
		Log.setPath(logPath);
		
		System.out.println("Enter path to the Knowledge Graph.");
		String pathToKG = scanner.nextLine();
		
		Model model = ModelFactory.createDefaultModel();
		if(pathToKG.endsWith(".ttl") || pathToKG.endsWith(".rdf") || pathToKG.endsWith(".nt") || pathToKG.endsWith(".owl") || pathToKG.endsWith(".jsonld") || pathToKG.endsWith(".n3"))
		{
			System.out.println("Enter path to the index of the Knowledge Graph.");
			String pathToIndex = scanner.nextLine();
			model = LoadRDF.readModel(pathToKG, pathToIndex);
		}
		if(pathToKG.endsWith(".hdt"))
		{
			model = LoadRDF.readHDT(pathToKG);
		}
		
		Logger logger = Log.getLogger();
		
		logger.info("Protocol run as: " + runningAs + "\n"
				+ "Used Data: " + pathToKG + "\n"
				+ "Log File: " + logPath);
		
		
		// --------------- Run the protocol as either party ---------------------
		if(runningAs.equals("seller"))
		{
			Model model2 = ModelProcessing.getFirstNStatements(model, modelsize);
			Seller.runAsSeller(model2, scanner);
		}
		if(runningAs.equals("buyer"))
		{
			Model model2 = ModelProcessing.getFirstNStatements(model, modelsize);
			Buyer.runAsBuyer(model2, scanner);
		}
		
		if(runningAs.equals("sellerNP"))
		{
			InsecureSeller.runAsSellerNoPrivacy(model, scanner);
		}
		
		if(runningAs.equals("buyerNP"))
		{
			InsecureBuyer.runAsBuyerNoPrivacy(model, scanner);
		}
	}
	
}
