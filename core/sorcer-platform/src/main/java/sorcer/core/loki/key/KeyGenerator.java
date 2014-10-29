package sorcer.core.loki.key;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;

import net.jini.id.Uuid;

/**
 * key generator for the implementation and operation of the
 * multi diffie hellman key exhange protocol in order to implement
 * groups in an ad hoc space computing environment
 * 
 * @author Daniel Kerr
 */

public class KeyGenerator implements KeyGenerationManagement
{
	//------------------------------------------------------------------------------------------------------------
	
	/** g component of multi diffie hellman key exchange */
	private static BigInteger g512 = new BigInteger(
            "153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b" +
            "212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc", 16);
    
	/** p component of multi diffie hellman key exchange */
	private static BigInteger p512 = new BigInteger(
            "9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31d" +
            "b8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b", 16);
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * generate key pair for multi diffie hellman key exchange
	 * 
	 * @return
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 */
	public KeyPair genKeyPair() throws InvalidAlgorithmParameterException,NoSuchAlgorithmException
	{
		DHParameterSpec dhParams = new DHParameterSpec(p512, g512);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhParams,new FixedRandom());
        
		return keyGen.generateKeyPair();
	}
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * generate key agreement based on key pair for multi diffie hellman key exchange
	 * 
	 * @param mkKP
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @see InvalidKeyException, NoSuchAlgorithmException
	 */
	public KeyAgreement genKeyAgreement(KeyPair myKP) throws InvalidKeyException,NoSuchAlgorithmException
	{
		KeyAgreement myKA;
		myKA = KeyAgreement.getInstance("DH");
		myKA.init(myKP.getPrivate());
		return myKA;
	}
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * generate complimentary compound key array for multi diffie hellman key exchange
	 * 
	 * @param ids
	 * @param pairs
	 * @return
	 * @throws InvalidKeyException
	 * @see InvalidKeyException
	 */
	public Map<Uuid,Key> genCompoundKeys(Uuid[] ids,KeyPair[] pairs) throws InvalidKeyException,NoSuchAlgorithmException
	{
		int N = pairs.length;
		KeyAgreement[] agrees = new KeyAgreement[N];
		for(int i=0;i<N;++i) { agrees[i] = genKeyAgreement(pairs[i]); }
		
		Map<Uuid,Key> compKeys = new HashMap<Uuid,Key>();
        for(int ind=0;ind<N;++ind)
        {
        	int nxt_ind = ((ind+1>pairs.length-1) ? 0 : ind+1);
        	compKeys.put(ids[ind],recKey(agrees,pairs,ind,nxt_ind));
        }
        
        return compKeys;
	}
	
	/**
	 * generate complimentary compound key array for multi diffie hellman key exchange
	 * 
	 * @param ids
	 * @param pairs
	 * @return
	 * @throws InvalidKeyException
	 * @see InvalidKeyException
	 */
	public Map<Uuid,Key> genCompoundKeys(Vector<Uuid> ids,Vector<KeyPair> pairs) throws InvalidKeyException,NoSuchAlgorithmException
	{
		int N = pairs.size();
		Vector<KeyAgreement> agrees = new Vector<KeyAgreement>();
		for(int i=0;i<N;++i) { agrees.add(genKeyAgreement(pairs.elementAt(i))); }
		
		Map<Uuid,Key> compKeys = new HashMap<Uuid,Key>();
        for(int ind=0;ind<N;++ind)
        {
        	int nxt_ind = ((ind+1>pairs.size()-1) ? 0 : ind+1);
        	compKeys.put(ids.elementAt(ind),recKey(agrees,pairs,ind,nxt_ind));
        }
        
        return compKeys;
	}
	
	/**
	 * recursive function neccesary for complimentary compound key creation
	 * 
	 * @param agrees
	 * @param pairs
	 * @param index
	 * @param cur
	 * @return
	 * @throws InvalidKeyException
	 */
    private static Key recKey(KeyAgreement[] agrees,KeyPair[] pairs,int index,int cur) throws InvalidKeyException
    {
        int next = (cur+1>pairs.length-1?0:cur+1);
        
        if(next==index)
        { return pairs[cur].getPublic(); }
        else
        { return agrees[cur].doPhase(recKey(agrees,pairs,index,next),false); }
    }

    private static Key recKey(Vector<KeyAgreement> agrees,Vector<KeyPair> pairs,int index,int cur) throws InvalidKeyException
    {
        int next = (cur+1>pairs.size()-1?0:cur+1);
        
        if(next==index)
        { return pairs.elementAt(cur).getPublic(); }
        else
        { return agrees.elementAt(cur).doPhase(recKey(agrees,pairs,index,next),false); }
    }
    
	//------------------------------------------------------------------------------------------------------------	
	
    /**
     * generate shared key for multi diffie hellman key exchange based on specific
	 * complimentary compound key
     * 
     * @param agree
     * @param compKey
     * @return
     * @throws InvalidKeyException
     * @see InvalidKeyException
     */
    public KeyAgreement genSharedKey(KeyAgreement agree,Key compKey) throws InvalidKeyException
	{
		agree.doPhase(compKey,true);
		return agree;
	}
    
	//------------------------------------------------------------------------------------------------------------
}