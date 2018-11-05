package statistics;

import java.util.HashMap;

/**
 * Stores handels to entropy stores
 * Stores can be called by name of entropy
 * @author ---
 *
 */
public class EntropyResults 
{
	private HashMap<EntropiesEnum, EntropyStore> entropies = new HashMap<EntropiesEnum, EntropyStore>();
	
	/**
	 * creates EntropyResults
	 */
	public EntropyResults()
	{
		
	}
	
	/**
	 * 
	 * @param name of entropy (EntropiesEnum)
	 * @return EntropyStore
	 */
	public EntropyStore getEntropyStore(EntropiesEnum name)
	{
		return entropies.get(name);
	}
	
	/**
	 * Creates new EntropyStore
	 * @param name of entropy (EntropiesEnum)
	 * @return new EntropyStore
	 */
	public EntropyStore newEntropy(EntropiesEnum name)
	{
		EntropyStore entropyStore = new EntropyStore();
		entropies.put(name, entropyStore);
		return entropyStore;
	}
	
}
