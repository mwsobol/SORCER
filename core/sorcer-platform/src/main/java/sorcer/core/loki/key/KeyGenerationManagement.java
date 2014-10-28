package sorcer.core.loki.key;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Vector;

import javax.crypto.KeyAgreement;

import net.jini.id.Uuid;

/**
 * The Key Generation Management provides the interface for
 * both key and key agremment generation, as well as the
 * framework specific implementation of the complimentary
 * compound key and the shared key object.
 * 
 * @author Daniel Kerr
 */

public interface KeyGenerationManagement
{
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * Generate key pair
	 * 
	 * @return the generated key pair
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchAlgorithmException
	 * @see InvalidAlgorithmParameterException, NoSuchAlgorithmException
	 */
	public KeyPair genKeyPair() throws InvalidAlgorithmParameterException,NoSuchAlgorithmException;
	/**
	 * Generate key agreement
	 * 
	 * @param myKP the previously generated key pair
	 * @return generated key agreement
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @see InvalidKeyException, NoSuchAlgorithmException
	 */
	public KeyAgreement genKeyAgreement(KeyPair myKP) throws InvalidKeyException,NoSuchAlgorithmException;
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * Generate complimentary compound keys
	 * 
	 * @param agrees
	 * @param pairs
	 * @return
	 * @throws InvalidKeyException;
	 * @see InvalidKeyException;
	 */
	public Map<Uuid,Key> genCompoundKeys(Uuid[] ids,KeyPair[] pairs) throws InvalidKeyException,NoSuchAlgorithmException;
	
	public Map<Uuid,Key> genCompoundKeys(Vector<Uuid> ids,Vector<KeyPair> pairs) throws InvalidKeyException,NoSuchAlgorithmException;
	/**
	 * Generate shared key based on the complimentary compound key
	 * 
	 * @param agree
	 * @param compKey
	 * @return
	 * @throws InvalidKeyException
	 * @see InvalidKeyException
	 */
	public KeyAgreement genSharedKey(KeyAgreement agree,Key compKey) throws InvalidKeyException;
	
	//------------------------------------------------------------------------------------------------------------
}
