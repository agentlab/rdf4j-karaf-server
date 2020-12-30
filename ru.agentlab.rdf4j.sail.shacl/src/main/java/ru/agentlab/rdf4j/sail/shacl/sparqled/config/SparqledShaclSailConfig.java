/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
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
