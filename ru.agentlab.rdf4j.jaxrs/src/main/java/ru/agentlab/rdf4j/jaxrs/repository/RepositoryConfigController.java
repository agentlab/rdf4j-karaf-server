package ru.agentlab.rdf4j.jaxrs.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = RepositoryConfigController.class, property = { "osgi.jaxrs.resource=true" })
//@Path("/rdf4j-server")
public class RepositoryConfigController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Reference
	private RepositoryManagerComponent repositoryManager;

	@GET
	@Path("/repconfigs")
    public String get(@QueryParam("type") String type) throws WebApplicationException {
		try (InputStream ttlInput = RepositoryConfig.class.getResourceAsStream(type + ".ttl")) {
			if(ttlInput == null) {
				throw new WebApplicationException("Template with type '" + type + "' not found", Response.Status.NOT_FOUND);
			}
			final String template = IOUtil.readString(new InputStreamReader(ttlInput, "UTF-8"));
			return template;
		} catch (IOException e) {
			logger.error("error while attempting to get template '" + type + "'", e);
			throw new WebApplicationException("error while attempting to get template: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@PUT
	@Path("/repconfigs/create")
    @Produces({"application/json", "application/sparql-results+json"})
    public boolean createRepository(@Context UriInfo ui) throws WebApplicationException {
		System.out.println("createRepository");
		String type = "";
		String repId = "";
		try {
			MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
			type = queryParams.get("type").get(0);
			repId = queryParams.get("Repository ID").get(0);

			System.out.println("RepositoryController.get");
			System.out.println("repId=" + repId);
			System.out.println("type=" + type);

			/*if ("federate".equals(type)) {
				rmf.addFed(repId, title, Arrays.asList(memberID), readonly, distinct);
			} else {*/
				Map m = convertMultiToRegularMap(queryParams);
				System.out.println("Map: " + m);
				ConfigTemplate ct = RepositoryManagerComponent.getConfigTemplate(type);
				System.out.println("ConfigTemplate: " + ct);
				String s = ct.render(convertMultiToRegularMap(queryParams));
				System.out.println("ConfigTemplate render: " + s);
				RepositoryConfig rc = repositoryManager.updateRepositoryConfig(s);
				System.out.println("RepositoryConfig.id: " + rc.getID().toString());
				System.out.println("RepositoryConfig: " + rc.toString());
			/*}*/
		} catch (RDF4JException e) {
			logger.error("error while attempting to get template '" + type + "'", e);
			throw new WebApplicationException("error while attempting to get template: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			logger.error("I/O error while attempting to create repository with id '" + repId + "' from template '" + type + "'", e);
			throw new WebApplicationException("error while attempting to get template: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
		return true;
	}

	private Map<String, String> convertMultiToRegularMap(MultivaluedMap<String, String> m) {
	    Map<String, String> map = new HashMap<String, String>();
	    if (m == null) {
	        return map;
	    }
	    for (Entry<String, List<String>> entry : m.entrySet()) {
	    	String val =entry.getValue().get(0);
	        map.put(entry.getKey(), val);
	    }
	    return map;
	}

}
