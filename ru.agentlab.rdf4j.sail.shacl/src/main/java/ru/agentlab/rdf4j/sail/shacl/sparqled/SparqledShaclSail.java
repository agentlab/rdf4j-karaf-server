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
