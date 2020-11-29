package ru.agentlab.rdf4j.sail.shacl.sparqled.impl;

import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ru.agentlab.rdf4j.sail.shacl.sparqled.config.SparqledShaclSailFactory;

public class Activator implements BundleActivator {
    SparqledShaclSailFactory sparqledShaclSailFactory;

	@Override
	public void start(BundleContext context) throws Exception {
		sparqledShaclSailFactory = new SparqledShaclSailFactory();
		SailRegistry.getInstance().add(sparqledShaclSailFactory);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	    if (sparqledShaclSailFactory != null) {
            SailRegistry.getInstance().remove(sparqledShaclSailFactory);
        }
	}
}
