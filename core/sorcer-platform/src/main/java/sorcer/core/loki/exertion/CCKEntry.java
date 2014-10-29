package sorcer.core.loki.exertion;

import java.security.Key;
import java.util.Map;

import net.jini.core.entry.Entry;
import net.jini.id.Uuid;

public class CCKEntry implements Entry {
	
	private static final long serialVersionUID = 323371062406569479L;
	
	public Map<Uuid, Key> ccKeys;

	static public CCKEntry get(Map<Uuid, Key> keys) {
		CCKEntry CCK = new CCKEntry();
		CCK.ccKeys = keys;
		return CCK;
	}

	public String getName() {
		return "Complimentary Compound Key Entry";
	}

}
