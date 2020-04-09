package ru.agentlab.rdf4j.jaxrs.repository.size;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;


/**
 * Handles requests for the size of (set of contexts in) a repository.
 * 
 */
@Component(service = SizeController.class, property = { "osgi.jaxrs.resource=true" })
//@Path("/rdf4j-server")
public class SizeController {
    //private static final Logger logger = LoggerFactory.getLogger(SizeController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    @GET
    @Path("/repositories/{repId}/size")
    public long getRepositorySize(@PathParam("repId") String repId, @QueryParam("context") String[] contextsStr) {
        Repository repository = repositoryManager.getRepository(repId);
        if(repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);
        
        ValueFactory vf = repository.getValueFactory();
        Resource[] contexts = Protocol.decodeContexts(contextsStr, vf);
        
        long size = -1;
        
        try (RepositoryConnection repositoryCon = repository.getConnection()) {
            size = repositoryCon.size(contexts);
        } catch (RepositoryException e) {
            throw new WebApplicationException("Repository error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
        }
        
        return size;
    }
}
