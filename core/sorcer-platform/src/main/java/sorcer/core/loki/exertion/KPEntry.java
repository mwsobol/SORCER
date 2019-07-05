package sorcer.core.loki.exertion;

import java.security.PublicKey;

import net.jini.core.entry.Entry;

public class KPEntry implements Entry {
	
	private static final long serialVersionUID = -3134975993027375539L;
	
	public Boolean isCreator;

	public byte[] keyPair;

	public PublicKey publicKey;

	public String GroupSeqId;

	static public KPEntry get(Boolean iscreator, byte[] keypair,
			PublicKey pk, String GSUID) {
		KPEntry KP = new KPEntry();
		KP.isCreator = iscreator;
		KP.keyPair = keypair;
		KP.publicKey = pk;
		KP.GroupSeqId = GSUID;
		return KP;
	}

	public String getName() {
		return "KeyPair and KeyAgreement Subroutine";
	}

}
