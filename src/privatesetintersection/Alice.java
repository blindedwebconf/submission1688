package privatesetintersection;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;

/**
 * This class represents Alice who can create an RSA keypair and can issue digital signatures
 */
public class Alice
{
    /**
     * Produces and returns an RSA keypair (N,e,d)
     * N: Modulus, e: Public exponent, d: Private exponent
     * The public exponent value is set to 65537 and the keylength to 2048
     * @return RSA keypair
     */
    public static KeyPair produceKeyPair()
    {
        try
        {
            KeyPairGenerator rsaKeyPairGenerator = KeyPairGenerator.getInstance("RSA");  //get rsa key generator

            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(2048, BigInteger.valueOf(65537)); //set the parameters for they key, key length=2048, public exponent=65537

            rsaKeyPairGenerator.initialize(spec); //initialise generator with the above parameters

            KeyPair keyPair = rsaKeyPairGenerator.generateKeyPair(); //generate the key pair, N:modulus, d:private exponent

            return (keyPair);  //return the key pair produced (N,e,d)

        } 
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Calculate mu' using the Chinese Remainder Theorem for optimization
     * Thanks to the isomorphism property f(x+y)=f(x)+f(y) we can split the mu^d modN in two:
     * one mode p , one mode q, and then we can combine the results to calculate muprime
     * @param mu
     * @param d
     * @param N
     * @param P
     * @param Q
     * @param PinverseModQ
     * @param QinverseModP
     * @return mu'
     */
    public static BigInteger calculateMuPrimeWithChineseRemainderTheorem(BigInteger mu, BigInteger d, BigInteger N, BigInteger P, BigInteger Q, BigInteger PinverseModQ, BigInteger QinverseModP)
    {
        try
        {
            BigInteger m1 = mu.modPow(d, N).mod(P); //calculate m1=(mu^d modN)modP

            BigInteger m2 = mu.modPow(d, N).mod(Q); //calculate m2=(mu^d modN)modQ

            //We combine the calculated m1 and m2 in order to calculate muprime
            //We calculate muprime: (m1*Q*QinverseModP + m2*P*PinverseModQ) mod N where N =P*Q
            
            BigInteger muprime = ((m1.multiply(Q).multiply(QinverseModP)).add(m2.multiply(P).multiply(PinverseModQ))).mod(N);

            return muprime;

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

}


