package ru.agentlab.rdf4j.jaxrs.sparql.providers;

import java.util.HashMap;
import java.util.Map;

public class TupleQueryResultModel {
	protected Map<String, Object> map = new HashMap<>();
	
	public Object put(String key, Object value) {
		return map.put(key, value);
	}
	
	public Object get(String key) {
		return map.get(key);
	}
}
