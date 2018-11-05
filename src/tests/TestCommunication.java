package tests;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import communication.Client;
import communication.Server;
import protocol.Log;

/**
 * Class containing tests regarding communication
 * 
 * @author ---
 *
 */
public class TestCommunication 
{
	
	/**
	 * Tests if a Bloom filter can be sent and received.
	 * @param runningAs
	 */
	public static void testBFCommunication(String runningAs)
	{
		Logger logger = Log.getLogger();
		
		if(runningAs.equals("seller"))
		{
			// Define how to enter an object into the BF (in this case a string)
			Funnel<String> statementSignatureFunnel = new Funnel<String>() 
			{
				  private static final long serialVersionUID = 1;
				  @Override
				  public void funnel(String statementSignature, PrimitiveSink into) 
				  {
				    into
				    	.putString(statementSignature, StandardCharsets.UTF_8);
				  }
			};
				
			// Create the bloom filter
			BloomFilter<String> bf = BloomFilter.create(statementSignatureFunnel, 10);
			bf.put("abcdefghij");
			bf.put("asdihgasädg");
			bf.put("a8wezhgnanve");
			bf.put("gha98wehae9");
			
			Server server = Server.getServer();
			server.sendObject(bf);

			logger.info("BloomFilter has been sent");
			
			// Sleep to make sure this program does not terminate before buyer had the chance to read the bf
			try {
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				logger.info("InteruptedException. Was trying to sleep for 20 seconds.");
				e.printStackTrace();
			}
		}
		if(runningAs.equals("buyer"))
		{
			Client client = Client.getClient();
			BloomFilter<String> received = client.<BloomFilter<String>>readObject();
			
			logger.info("Received BF size: " + received.approximateElementCount());
			logger.info("Containes abcdefghij: " + received.mightContain("abcdefghij"));
			logger.info("Contains 'nonsense': " + received.mightContain("nonsense"));
		}
	}

}
