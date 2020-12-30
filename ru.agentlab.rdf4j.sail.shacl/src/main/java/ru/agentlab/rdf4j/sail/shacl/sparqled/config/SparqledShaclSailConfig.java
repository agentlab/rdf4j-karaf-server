/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
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
