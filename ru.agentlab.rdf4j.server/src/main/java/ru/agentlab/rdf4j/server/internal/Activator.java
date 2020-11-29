package ru.agentlab.rdf4j.server.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ru.agentlab.rdf4j.server.bp.BlueprintApplicationContext;

public class Activator implements BundleActivator {
	BundleContext bundleContext;
	BlueprintApplicationContext blueprintApplicationContext;

	@Override
	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;
		blueprintApplicationContext = new BlueprintApplicationContext(bundleContext);
       	blueprintApplicationContext.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		blueprintApplicationContext.stop();
	}
}
