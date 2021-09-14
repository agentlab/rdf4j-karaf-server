/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
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
       	
       	ClassLoader myClassClassLoader = Activator.class.getClassLoader();
       	System.out.println("ClassLoader of BasicOneActivator is " + myClassClassLoader);
       	
       	Class<?> classA = myClassClassLoader.loadClass("org.eclipse.rdf4j.repository.config.RepositoryConfigException");
       	System.out.println("ClassLoader of BasicOneActivator is " + classA);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		blueprintApplicationContext.stop();
	}
}
