/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
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
