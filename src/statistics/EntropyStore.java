package statistics;

import java.util.HashMap;

/**
 * Stores entropy values and multiset for an entropy
 * @author ---
 *
 */
public class EntropyStore {
	
	private double entropy = Double.MIN_VALUE;					// entropy for unified models
	private double entropyBuyer = Double.MIN_VALUE;				// entropy for buyer model
	private double entropyGain = Double.MIN_VALUE;				// entropy difference between unified and buyer model
	private double entropySeller = Double.MIN_VALUE;			// entropy for seller model
	private HashMap<String, Integer> sellerMultiset = null;		// multiset of seller
	
	public EntropyStore()
	{
		
	}

	public double getEntropy() 
	{
		return entropy;
	}

	public void setEntropy(double entropy)
	{
		this.entropy = entropy;
	}

	public double getEntropyBuyer() 
	{
		return entropyBuyer;
	}

	public void setEntropyBuyer(double entropyBuyer) 
	{
		this.entropyBuyer = entropyBuyer;
	}

	public double getEntropyGain() 
	{
		return entropyGain;
	}

	public void setEntropyGain(double entropyGain) 
	{
		this.entropyGain = entropyGain;
	}
	
	public double getEntropySeller() 
	{
		return entropySeller;
	}

	public void setEntropySeller(double entropySeller) 
	{
		this.entropySeller = entropySeller;
	}

	public HashMap<String, Integer> getSellerMultiset() 
	{
		return sellerMultiset;
	}

	public void setSellerMultiset(HashMap<String, Integer> sellerMultiset) 
	{
		this.sellerMultiset = sellerMultiset;
	}

	
}
