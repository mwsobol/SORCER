package sorcer.core.loki.member;

import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.space.JavaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ArrayContext;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.loki.crypt.EncryptionManagement;
import sorcer.core.loki.crypt.EncryptionManager;
import sorcer.core.loki.exertion.CCKEntry;
import sorcer.core.loki.exertion.KPEntry;
import sorcer.core.loki.group.GroupManagement;
import sorcer.core.loki.key.KeyGenerationManagement;
import sorcer.core.loki.key.KeyGenerator;
import sorcer.service.Accessor;
import sorcer.service.ContextException;
import sorcer.service.Routine;
import sorcer.service.space.SpaceAccessor;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.security.*;
import java.sql.Timestamp;
import java.util.Vector;

/**
 * @author Daniel Kerr
 */

public class LokiMemberUtil {
	//------------------------------------------------------------------------------------------------------------
	/** encryption manager */
	private EncryptionManagement myCryptMan;
	/** key generation manager */
	private KeyGenerationManagement myKeyGen;

	/** key agreement */
	private KeyAgreement myKeyAgreement;
	/** key pair */
	private KeyPair myKeyPair;
	/** unique sequence identifier */
	private String memberSeqId;
	/** unique identifier */
	private Uuid myUID;
	/** key identifier */
	private String myName;
	/** Group sequence identifier */
	private String groupSeqId;
	/** Group Tag */
	private String groupName;
	
	/** debug switch */
	private boolean debug = true;
	/** debug db switch */
	private boolean debugdb = true;
	/** Logger object */
	protected static Logger logger = LoggerFactory.getLogger(LokiMemberUtil.class.getName());
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * initializes all local variables and retrieves the
	 * compound key, and utilizes it to compute the shared
	 * key
	 */
	public LokiMemberUtil(String name)
	{
		try
		{
			if(debug)
	        { logger.info("START INITIALIZING LOKI MEMBER UTILITY"); }
			
			memberSeqId = null;
			myUID = UuidFactory.generate();
			myName = name;
			groupSeqId = null;
			groupName = null;
			
			myKeyGen = new KeyGenerator();
			myKeyPair = myKeyGen.genKeyPair();
			myKeyAgreement = myKeyGen.genKeyAgreement(myKeyPair);
			
			myCryptMan = new EncryptionManager();
			
			if(debug)
			{ logger.info("FINISHED INITIALIZING LOKI MEMBER UTILITY"); }
		}
		catch(InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException e) { e.printStackTrace(); }
	}
	
	public void setGroupSeqId(String GSUID)
	{ groupSeqId = GSUID; }
	
	public void setGroupName(String gname)
	{ groupName = gname; }
	
	//------------------------------------------------------------------------------------------------------------
	
	public CCKEntry readCCK(Class serviceType)
	{
		try
		{
			JavaSpace space = SpaceAccessor.getSpace();
			if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }
			
			GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
			if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			ExertionEnvelop cckeeTemp = ExertionEnvelop.getTemplate();
	        cckeeTemp.isEncrypted = false;
	        cckeeTemp.serviceType = serviceType;
//	        cckeeTemp.serviceInfo = "ComplimentaryCompoundKeys:"+serviceInfo.getName();
//	        cckeeTemp.exertionID = "ComplimentaryCompoundKeys:"+serviceInfo.getName();
	        
	        if(debug)
	        { logger.info(printEE("LOOKING FOR CCK EXERTION",cckeeTemp)); }
			
			ExertionEnvelop cckeeRes = (ExertionEnvelop)space.read(cckeeTemp,null,Lease.FOREVER);
			CCKEntry cckeRes = (CCKEntry)cckeeRes.exertion;
			
			if(debug)
			{
				logger.info("GOT CCK EXERTION =======> "
				+"\nCCK : "+cckeRes.ccKeys.get(serviceType));
			}
			
			myCryptMan.init(myKeyGen.genSharedKey(myKeyAgreement,cckeRes.ccKeys.get(myUID)));
			
			if(debug)
			{ logger.info("INITIALIZED MEMBER ENCRYPTION MANAGER"); }
			
			return cckeRes;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public KPEntry readKP(Class serviceType,Vector<Uuid> ids,Vector<KeyPair> r_pairs)
	{
		try
		{
			JavaSpace space = SpaceAccessor.getSpace();
			if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }
			
			GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
			if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			ExertionEnvelop kpeeTemp = ExertionEnvelop.getTemplate();
		    kpeeTemp.isEncrypted = false;
		    //kpeeTemp.serviceInfo = "MemberKeyPairKeyAgreement:"+serviceInfo;
		    kpeeTemp.serviceType = serviceType;
	
		    if(debug)
		    { logger.info(printEE("LOOKING FOR KEY PAIR EXERTION",kpeeTemp)); }
	
			ExertionEnvelop kpeeRes = (ExertionEnvelop)space.read(kpeeTemp,null,Lease.FOREVER);
	    	KPEntry kpeRes = (KPEntry)kpeeRes.exertion;
	
	    	//--------------------------------------
	    	KeyGenerator kg = new KeyGenerator();
	    	KeyAgreement localKA = kg.genKeyAgreement(myKeyPair);
	    	localKA.doPhase(kpeRes.publicKey, true);
	        
	    	Cipher deCipher = Cipher.getInstance("DES");
	        deCipher.init(Cipher.DECRYPT_MODE,localKA.generateSecret("DES"));
	        
	        ByteArrayInputStream l_bais = new ByteArrayInputStream(deCipher.doFinal(kpeRes.keyPair));
	    	ObjectInputStream l_ois = new ObjectInputStream(l_bais);
			MarshalledObject mo = (MarshalledObject) l_ois.readObject();
			//---------------
			
			ids.add(kpeeRes.exertionID);
	    	r_pairs.add((KeyPair)mo.get());
	    	
	    	return kpeRes;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public CCKEntry takeCCK(Class serviceType)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			ExertionEnvelop cckeeTemp = ExertionEnvelop.getTemplate();
	        cckeeTemp.isEncrypted = false;
	        cckeeTemp.serviceType = serviceType;
//	        cckeeTemp.serviceInfo = "ComplimentaryCompoundKeys:"+serviceInfo;
//	        cckeeTemp.exertionID = "ComplimentaryCompoundKeys:"+serviceInfo;
	        
	        if(debug)
	        { logger.info(printEE("LOOKING FOR CCK EXERTION",cckeeTemp)); }
			
			ExertionEnvelop cckeeRes = (ExertionEnvelop)space.take(cckeeTemp,null,Lease.FOREVER);
			CCKEntry cckeRes = (CCKEntry)cckeeRes.exertion;
			
			if(debug)
			{
				logger.info("GOT CCK EXERTION =======> "
				+"\nCCK : "+cckeRes.ccKeys.get(serviceType));
			}
			
			myCryptMan.init(myKeyGen.genSharedKey(myKeyAgreement,cckeRes.ccKeys.get(myUID)));
			
			if(debug)
			{ logger.info("INITIALIZED MEMBER ENCRYPTION MANAGER"); }
			
			return cckeRes;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public KPEntry takeKP(Class serviceType,Vector<Uuid> ids,Vector<KeyPair> r_pairs)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			ExertionEnvelop kpeeTemp = ExertionEnvelop.getTemplate();
		    kpeeTemp.isEncrypted = false;
		    //kpeeTemp.serviceInfo = "MemberKeyPairKeyAgreement:"+serviceInfo;
		    kpeeTemp.serviceType = serviceType;
	
		    if(debug)
		    { logger.info(printEE("LOOKING FOR KEY PAIR EXERTION",kpeeTemp)); }
	
			ExertionEnvelop kpeeRes = (ExertionEnvelop)space.take(kpeeTemp,null,Lease.FOREVER);
	    	KPEntry kpeRes = (KPEntry)kpeeRes.exertion;
	
	    	//--------------------------------------
	    	KeyGenerator kg = new KeyGenerator();
	    	KeyAgreement localKA = kg.genKeyAgreement(myKeyPair);
	    	localKA.doPhase(kpeRes.publicKey, true);
	        
	    	Cipher deCipher = Cipher.getInstance("DES");
	        deCipher.init(Cipher.DECRYPT_MODE,localKA.generateSecret("DES"));
	        
	        ByteArrayInputStream l_bais = new ByteArrayInputStream(deCipher.doFinal(kpeRes.keyPair));
	    	ObjectInputStream l_ois = new ObjectInputStream(l_bais);
			MarshalledObject mo = (MarshalledObject) l_ois.readObject();
			//---------------
			
			ids.add(kpeeRes.exertionID);
	    	r_pairs.add((KeyPair)mo.get());
	    	
	    	return kpeRes;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public ExertionEnvelop takewriteCCKExertion(Class serviceType)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			Vector<Uuid> ids = new Vector<Uuid>();
			Vector<KeyPair> r_pairs = new Vector<KeyPair>();
	        
			ids.add(myUID);
	        r_pairs.add(myKeyPair);
	        
	        /*----------------------------*/
	        readKP(serviceType,ids,r_pairs);
	        /*----------------------------*/
	        
	        ExertionEnvelop cckEE = ExertionEnvelop.getTemplate();
	    	cckEE.isEncrypted = false;
//	    	cckEE.serviceInfo = "ComplimentaryCompoundKeys:"+serviceInfo;
//	    	cckEE.exertionID = "ComplimentaryCompoundKeys:"+serviceInfo;
	    	cckEE.serviceType = serviceType;
	    	space.takeIfExists(cckEE,null,Lease.FOREVER);
	        
	    	cckEE.entry = CCKEntry.get(myKeyGen.genCompoundKeys(ids,r_pairs));
	        space.write(cckEE,null,Lease.FOREVER);
	        
	        if(debug)
	        { logger.info(printEE("COMPLIMENTARY COMPOUND KEY EXERTION CONTAINS",cckEE)); }
			
			myCryptMan.init(myKeyGen.genSharedKey(myKeyAgreement,((CCKEntry)cckEE.exertion).ccKeys.get(myUID)));
			
			return cckEE;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public ExertionEnvelop takewriteCKPExertion(Class serviceType)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			/*----------------------------*/
			if(debugdb)
			{
				ArrayContext context1 = new ArrayContext();
		        ArrayContext context1r = new ArrayContext();
				try
				{
					context1.iv(0,"Member : "+myName);
					context1.iv(1,myKeyPair.getPublic().toString());
					context1.iv(2,myKeyAgreement.toString());
					context1.iv(3,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(4,myName);
					context1.iv(5,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(6,myName);
					
					context1r = (ArrayContext)groupMan.addMemberEntry(context1);
					memberSeqId = context1r.ov(0).toString();
					logger.info("Group Member Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
	
		        ArrayContext context2 = new ArrayContext();
		        ArrayContext context2r = new ArrayContext();
				try
				{
					context2.iv(0,memberSeqId);
					context2.iv(1,groupName);
					context2.iv(2,(new Timestamp(System.currentTimeMillis())).toString());
					context2.iv(3,"This is my comment");
					context2.iv(4,(new Timestamp(System.currentTimeMillis())).toString());
					context2.iv(5,myName);
					context2.iv(6,(new Timestamp(System.currentTimeMillis())).toString());
					context2.iv(7,myName);
					
					context2r = (ArrayContext)groupMan.addGroupEntry(context2);
					groupSeqId = context2r.ov(0).toString();
					logger.info("Group Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
	
				ArrayContext context3 = new ArrayContext();
				try
				{
					context3.iv(0,groupSeqId);
					context3.iv(1,memberSeqId);
					
					groupMan.addMembershipEntry(context3);
					logger.info("Membership Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
			}
			/*----------------------------*/

			ExertionEnvelop ckpee = ExertionEnvelop.getTemplate();
	        ckpee.isEncrypted = false;
	        ckpee.serviceType = serviceType;
	        //ckpee.exertionID = "CreatorsPublicKey";
	        ckpee.exertionID = UuidFactory.generate();
			space.takeIfExists(ckpee,null,Lease.FOREVER);
			
	        ckpee.entry = KPEntry.get(true,null,myKeyPair.getPublic(),groupSeqId);
			space.write(ckpee,null,Lease.FOREVER);
			
			if(debug)
			{ logger.info(printEE("CREATOR KEY PAIR EXERTION CONTAINS",ckpee)); }
			
			return ckpee;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public ExertionEnvelop takewriteKPExertion(Key creatorPublicKey, Class serviceType)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
	        /*----------------------------*/
	        if(debugdb)
	        {
				ArrayContext context1 = new ArrayContext();
		        ArrayContext context1r = new ArrayContext();
				try
				{
					context1.iv(0,"Member : "+myName);
					context1.iv(1,myKeyPair.getPublic().toString());
					context1.iv(2,myKeyAgreement.toString());
					context1.iv(3,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(4,myName);
					context1.iv(5,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(6,myName);
	
					context1r = (ArrayContext)groupMan.addMemberEntry(context1);
					memberSeqId = context1r.ov(0).toString();
					logger.info("Member Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
				
				ArrayContext context2 = new ArrayContext();
				try
				{
					context2.iv(0,groupSeqId);
					context2.iv(1,memberSeqId);
					
					groupMan.addMembershipEntry(context2);
					logger.info("Membership Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
	        }
			/*----------------------------*/

			KeyGenerator kg = new KeyGenerator();
	        KeyAgreement localKA = kg.genKeyAgreement(myKeyPair);
	        localKA.doPhase(creatorPublicKey, true);
	        
	        Cipher enCipher = Cipher.getInstance("DES");
	        enCipher.init(Cipher.ENCRYPT_MODE,localKA.generateSecret("DES"));
	        
	        ByteArrayOutputStream l_baos = new ByteArrayOutputStream();
			ObjectOutputStream l_oos = new ObjectOutputStream(l_baos);
			l_oos.writeObject(new MarshalledObject(myKeyPair));
			
			ExertionEnvelop kpEE = ExertionEnvelop.getTemplate();
			kpEE.isEncrypted = false;
			//kpEE.serviceInfo = "MemberKeyPairKeyAgreement:"+serviceInfo;
			kpEE.serviceType = serviceType;
	        kpEE.exertionID = myUID;
	    	space.takeIfExists(kpEE,null,Lease.FOREVER);
	        kpEE.entry = KPEntry.get(false,enCipher.doFinal(l_baos.toByteArray()),myKeyPair.getPublic(),groupName);
	        space.write(kpEE,null,Lease.FOREVER);
			
	        if(debug)
	        { logger.info(printEE("WRITING MY KEY PAIR EXERTION",kpEE)); }
	        
	        return kpEE;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	//------------------------------------------------------------------------------------------------------------

	public ExertionEnvelop readEnEE(ExertionEnvelop template,Transaction txn)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			template.isEncrypted = true;
			
			if(debug)
			{ logger.info(printEE("LOOKING FOR EXERTION ENVELOP",template)); }
			
			ExertionEnvelop ee = (ExertionEnvelop) space.read(template, txn, Long.MAX_VALUE);
			
			if(debug)
			{
				logger.info(printEE("RECIEVED ENCRYPTED EXERTION ENVELOP",ee));
				logger.info("START DECRYPTING");
			}
			
			ByteArrayInputStream bais = new ByteArrayInputStream(myCryptMan.decrypt(ee.encryptedExertion));
			ObjectInputStream ois = new ObjectInputStream(bais);
			MarshalledObject mo = (MarshalledObject) ois.readObject();
			
			ee.isEncrypted = false;
			ee.encryptedExertion = null;
			ee.exertion = (Routine) mo.get();
			
			if(debug)
			{ logger.info("FINISHED DECRYPTING"); }

			/*----------------------------*/
			if(debugdb)
			{
				ArrayContext context1 = new ArrayContext();
		        ArrayContext context1r = new ArrayContext();
				try
				{
					context1.iv(0,"Read");
					context1.iv(1,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(2,"This is my Read Action");
					
					context1r = (ArrayContext)groupMan.addActivityEntry(context1);
					logger.info("Activity Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
	
		        ArrayContext context2 = new ArrayContext();
				try
				{
					context2.iv(0,context1r.ov(0));
					context2.iv(1,memberSeqId);
					
					groupMan.addExecutionEntry(context2);
					logger.info("Execution Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
			}
			/*----------------------------*/
			
			return ee;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public ExertionEnvelop takeEnEE(ExertionEnvelop template,Transaction txn)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			template.isEncrypted = true;
			
			if(debug)
			{ logger.info(printEE("LOOKING FOR EXERTION ENVELOP",template)); }
			
			ExertionEnvelop ee = (ExertionEnvelop) space.take(template, txn, Long.MAX_VALUE);

			if(debug)
			{
				logger.info(printEE("RECIEVED ENCRYPTED EXERTION ENVELOP",ee));
				logger.info("START DECRYPTING");
			}
			
			ByteArrayInputStream bais = new ByteArrayInputStream(myCryptMan.decrypt(ee.encryptedExertion));
			ObjectInputStream ois = new ObjectInputStream(bais);
			MarshalledObject mo = (MarshalledObject) ois.readObject();
			
			ee.isEncrypted = false;
			ee.encryptedExertion = null;
			ee.exertion = (Routine) mo.get();
			
			if(debug)
			{ logger.info("FINISHED DECRYPTING"); }
			
			/*----------------------------*/
			if(debugdb)
			{
		        ArrayContext context1 = new ArrayContext();
		        ArrayContext context1r = new ArrayContext();
				try
				{
					context1.iv(0,"Take");
					context1.iv(1,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(2,"This is my Take Action");
					
					context1r = (ArrayContext)groupMan.addActivityEntry(context1);
					logger.info("Activity Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
	
		        ArrayContext context2 = new ArrayContext();
				try
				{
					context2.iv(0,context1r.ov(0));
					context2.iv(1,memberSeqId);
					
					groupMan.addExecutionEntry(context2);
					logger.info("Execution Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
			}
			/*----------------------------*/
			
			return ee;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	public ExertionEnvelop writeEnEE(ExertionEnvelop template)
	{
		try
		{
            JavaSpace space = SpaceAccessor.getSpace();
            if(space == null)			{ throw new Exception("NO SPACE FOUND!!!"); }

            GroupManagement groupMan = Accessor.get().getService(null, GroupManagement.class);
            if(groupMan == null)		{ throw new Exception("NO GROUP MANAGER FOUND!!!"); }
			
			if(debug)
			{ logger.info("START ENCRYPTING"); }
			
			/*----------------------------*/
			if(debugdb)
			{
				ArrayContext context1 = new ArrayContext();
		        ArrayContext context1r = new ArrayContext();
				try
				{
					context1.iv(0,groupSeqId);
					context1.iv(1,template.exertion.getName());
					context1.iv(2,template.exertion.getSelectedFidelity().toString());
					context1.iv(3,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(4,myName);
					context1.iv(5,(new Timestamp(System.currentTimeMillis())).toString());
					context1.iv(6,myName);
					
					context1r = (ArrayContext)groupMan.addExertionEntry(context1);
					logger.info("Routine Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
				
		        ArrayContext context2 = new ArrayContext();
		        ArrayContext context2r = new ArrayContext();
				try
				{
					context2.iv(0,"Write");
					context2.iv(1,(new Timestamp(System.currentTimeMillis())).toString());
					context2.iv(2,"This is my Write Action");
					
					context2r = (ArrayContext)groupMan.addActivityEntry(context2);
					logger.info("Activity Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
	
		        ArrayContext context3 = new ArrayContext();
				try
				{
					context3.iv(0,context2r.ov(0));
					context3.iv(1,context1r.ov(0));
					context3.iv(2,memberSeqId);
					
					groupMan.addExecutionEntry(context3);
					logger.info("Execution Insertion Complete...");
				}
				catch(ContextException e) { e.printStackTrace(); }
				catch(RemoteException e) { e.printStackTrace(); }
			}
			/*----------------------------*/

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(new MarshalledObject(template.exertion));
			byte[] ba = baos.toByteArray();
			
			template.encryptedExertion = myCryptMan.encrypt(ba);
			template.exertion = null;
			template.isEncrypted = true;
			
			if(debug)
			{
				logger.info("START ENCRYPTING");
				logger.info(printEE("WRITING EXERTION THAT CONTAINS",template));
			}
	
			space.write(template, null, Lease.FOREVER);
			
			return template;
		}
		catch(Exception e)
		{ e.printStackTrace(); }
		
		return null;
	}
	
	//------------------------------------------------------------------------------------------------------------
	
	private String printEE(String header,ExertionEnvelop ee)
	{
		return header + " =======> "
		+ "\n\tEncrypted Routine = " + ee.encryptedExertion
		+ "\n\tRoutine = " + ee.exertion
		+ "\n\tExertionID = " + ee.exertionID
		+ "\n\tIs Encrypted = " + ee.isEncrypted
		+ "\n\tIs Job = " + ee.isJob
		+ "\n\tParentID = " + ee.parentID
		+ "\n\tProvider Tag = " + ee.providerName
		+ "\n\tProvider Subject = " + ee.providerSubject
		+ "\n\tService Type = " + ee.serviceType
		+ "\n\tState = " + ee.state;
	}
	
	//------------------------------------------------------------------------------------------------------------
}
