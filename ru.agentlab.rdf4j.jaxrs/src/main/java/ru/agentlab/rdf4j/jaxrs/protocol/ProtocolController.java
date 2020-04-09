package ru.agentlab.rdf4j.jaxrs.protocol;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.osgi.service.component.annotations.Component;

/**
 * Handles requests for protocol information. Currently returns the protocol version as plain text.
 */
@Component(service = ProtocolController.class, property = { "osgi.jaxrs.resource=true" })
//@Path("/rdf4j-server")
public class ProtocolController {

	@GET
	@Path("/protocol")
	public String getProtocolVersion() throws Exception {
		return Protocol.VERSION;
	}
}
