/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.sail.shacl.sparqled;

import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

public class SparqledShaclSail extends ShaclSail {
	
	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		SparqledShaclSailConnection shaclSailConnection = new SparqledShaclSailConnection(super.getConnection());
		return shaclSailConnection;
	}

}
