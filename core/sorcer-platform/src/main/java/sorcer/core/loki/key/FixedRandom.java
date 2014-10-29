package sorcer.core.loki.key;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Is used in generating a random component for cryptographic
 * functions, ciphering, and key component creation
 * 
 * @author Daniel Kerr
 */

public class FixedRandom extends SecureRandom
{
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * constructor initializes all local objects
	 */
	FixedRandom()
	{
		try
		{
			this.sha = MessageDigest.getInstance("SHA-1");
			this.state = sha.digest();
		}
		catch (NoSuchAlgorithmException e)
		{ throw new RuntimeException("can't find SHA-1!"); }
	}
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * inherited method modified to fix length of randomization
	 * 
	 * @param bytes
	 */
	public void nextBytes(byte[] bytes)
	{
		int off = 0;
		sha.update(state);
		
		while(off < bytes.length)
		{	            
			state = sha.digest();
			
			if(bytes.length - off > state.length)
			{ System.arraycopy(state, 0, bytes, off, state.length); }
			else
			{ System.arraycopy(state, 0, bytes, off, bytes.length - off); }
			
			off += state.length;
			sha.update(state);
		}
	}
	
	//------------------------------------------------------------------------------------------------------------
	
	/** message digest */
	MessageDigest sha;
	/** state byte object generated from digest */
	byte[] state;
	
	//------------------------------------------------------------------------------------------------------------
}