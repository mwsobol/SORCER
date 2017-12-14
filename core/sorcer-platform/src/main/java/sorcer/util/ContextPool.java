package sorcer.util;

import sorcer.service.Context;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContextPool extends Pool<String, Set<String>> {

	private Map<String, Context> contextCache;

	public Map<String, Context> getContextCache() {
		return contextCache;
	}

	public void setContextCache(Map<String, Context> contextCache) {
		this.contextCache = contextCache;
	}

	public void put(String entName, String... depNames) {
		Set<String> deps = new HashSet();
		for (String dep: depNames) {
			deps.add(dep);
		}
		put(entName, deps);
	}

	public void putContext(String ent, Context context) {
		contextCache.put(ent, context);
	}

	public Context getContext(String ent) {
		return contextCache.get(ent);
	}

	public Context getContext(String ent, String... deps) {
		boolean isCached = false;
		Set<String> depNames = get(ent);
		for (String dep : deps) {
			if (depNames.contains(dep)) {
				isCached = true;
			} else {
				isCached = false;
				break;
			}
		}
		Context cxt = contextCache.get(ent);
		if (isCached  && cxt.isValid()) {
			return contextCache.get(ent);
		} else {
			return null;
		}
	}
}
