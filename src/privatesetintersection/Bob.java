package privatesetintersection;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import protocol.Log;
import protocol.Tuple;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

/**
 * The class Bob represents Bob who wishes to get a signature from Alice over his message
 * but without Alice seeing the actual message
 */
public class Bob
{

    /**
     * Calculates and returns the mu
     * Bob uses ALice's public key and a random value r, such that r is relatively prime to N
     * to compute the blinding factor r^e mod N. Bob then computes the blinded message mu = H(msg) * r^e mod N
     * It is important that r is a random number so that mu does not leak any information about the actual message
     * @param input String to be signed
     * @param publicKey
     * @return the blinded message mu
     */
	public static Tuple<BigInteger, BigInteger> calculateMu(String input, RSAPublicKey publicKey)
    {
    	BigInteger r;
        try
        {
        	String message = DigestUtils.sha512Hex(input);

            byte[] msg = message.getBytes("UTF8"); //get the bytes of the hashed message

            BigInteger m = new BigInteger(msg);  //create a BigInteger object based on the extracted bytes of the message

            BigInteger e = publicKey.getPublicExponent(); //get the public exponent 'e' of Alice's key pair
            
            BigInteger N = publicKey.getModulus();

            // Generate a random number so that it belongs to Z*n and is >1 and therefore r is invertible in Z*n
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

            byte[] randomBytes = new byte[10]; //create byte array to store the r

            BigInteger one = new BigInteger("1"); // make BigInteger object equal to 1, so we can compare it later with the r produced to verify r>1

            BigInteger gcd = null; // initialise variable gcd to null

            do
            {
                random.nextBytes(randomBytes); //generate random bytes using the SecureRandom function

                r = new BigInteger(randomBytes); //make a BigInteger object based on the generated random bytes representing the number r

                gcd = r.gcd(publicKey.getModulus()); //calculate the gcd for random number r and the  modulus of the keypair
            }
            while (!gcd.equals(one) || r.compareTo(N) >= 0 || r.compareTo(one) <= 0); //repeat until getting an r that satisfies all the conditions and belongs to Z*n and >1

            //now that we got an r that satisfies the restrictions described we can proceed with calculation of mu

            BigInteger mu = ((r.modPow(e, N)).multiply(m)).mod(N); //Bob computes mu = H(msg) * r^e mod N

            return new Tuple<BigInteger, BigInteger>(mu, r);

        } 
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Calculate signature over mu'
     * Bob receives the signature over the blinded message that he sent to Alice
     * and removes the blinding factor to compute the signature over his actual message
     * @param muprime
     * @param N
     * @param r
     * @return signature
     */
    public static String signatureCalculation(BigInteger muprime, BigInteger N, BigInteger r)
    {
        try
        {
            BigInteger s = r.modInverse(N).multiply(muprime).mod(N); //Bob computes sig = mu'*r^-1 mod N, inverse of r mod N multiplied with muprime mod N, to remove the blinding factor

            byte[] bytes = new Base64().encode(s.toByteArray()); //encode with Base64 encoding to be able to read all the symbols

            String signature = (new String(bytes)); //make a string based on the byte array representing the signature

            return signature; 
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if the signature received from Alice, is a valid signature for the message given, this can be easily computed because(m^d)^e modN = m
     * @param signature
     * @param publicKey
     * @param originalStatement
     * @return true if singature is valid, false otherwise
     */
    public static boolean verify(String signature, RSAPublicKey publicKey, String originalStatement)
    {
    	Logger logger = Log.getLogger();
    	
        try
        {
        	String message = DigestUtils.sha512Hex(originalStatement);

            byte[] msg = message.getBytes("UTF8"); //get the bytes of the hashed message

            BigInteger m = new BigInteger(msg);  //create a BigInteger object based on the extracted bytes of the message
        	
            byte[] bytes = signature.getBytes(); //create a byte array extracting the bytes from the signature

            byte[] decodedBytes = new Base64().decode(bytes); // decode the bytes with Base64 decoding (remember we encoded with base64 earlier)

            BigInteger sig = new BigInteger(decodedBytes); // create the BigInteger object based on the bytes of the signature

            BigInteger e = publicKey.getPublicExponent();//get the public exponent of Alice's key pair
            
            BigInteger N = publicKey.getModulus();

            BigInteger signedMessageBigInt = sig.modPow(e, N); //calculate sig^e modN, if we get back the initial message that means that the signature is valid, this works because (m^d)^e modN = m

            String signedMessage = new String(signedMessageBigInt.toByteArray()); //create a String based on the result of the above calculation

            String initialMessage = new String(m.toByteArray()); //create a String based on the initial message we wished to get a signature on

            if (signedMessage.equals(initialMessage)) //compare the two Strings, if they are equal the signature we got is a valid
            {
            	return true;
            } 
            else
            {
            	return false;
            }
        } 
        catch (Exception e)
        {
        	logger.info("Exception in Bob verify, when trying to verify a blind signature. \n" 
					+ e.getMessage() + "\n"
					+ e.toString() + "\n"
					+ "Protocol is being terminated.");
			System.exit(1);
        }
        
        return false;
    }

}

