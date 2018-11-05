package dataquality;

import java.math.BigInteger;
import java.util.LinkedList;

/**
 * Class stores information about AES encryption.
 * These are: Used keys, resulting encryptions, and CBC Initialization Vector
 * @author ---
 *
 */
public class EncryptionStorage 
{
	private byte[] byteIV;
	private LinkedList<byte[]> encryptedParts;
	private LinkedList<BigInteger> keys;
	
	public EncryptionStorage()
	{
		
	}

	public byte[] getByteIV() 
	{
		return byteIV;
	}

	public void setByteIV(byte[] byteIV) 
	{
		this.byteIV = byteIV;
	}

	public LinkedList<byte[]> getEncryptedParts() 
	{
		return encryptedParts;
	}

	public void setEncryptedParts(LinkedList<byte[]> encryptedParts) 
	{
		this.encryptedParts = encryptedParts;
	}

	public LinkedList<BigInteger> getKeys() 
	{
		return keys;
	}

	public void setKeys(LinkedList<BigInteger> keys) 
	{
		this.keys = keys;
	}
	
	
}
