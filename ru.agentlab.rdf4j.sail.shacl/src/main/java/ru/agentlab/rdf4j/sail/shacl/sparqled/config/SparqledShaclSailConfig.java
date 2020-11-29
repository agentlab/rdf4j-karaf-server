package ru.agentlab.rdf4j.sail.shacl.sparqled.config;

import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;


public class SparqledShaclSailConfig extends ShaclSailConfig {

	public SparqledShaclSailConfig() {
		super();
		// redefine SAIL type
		setType(SparqledShaclSailFactory.SAIL_TYPE);
	}

	public SparqledShaclSailConfig(SailImplConfig delegate) {
		super(delegate);
		// redefine SAIL type
        setType(SparqledShaclSailFactory.SAIL_TYPE);
	}
}
