package ru.agentlab.rdf4j.jaxrs.repository.namespaces;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;

import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.repository.ProtocolUtils;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = NamespaceController.class, property = { "osgi.jaxrs.resource=true" })
//@Path("/rdf4j-server")
public class NamespaceController {
    private static final Logger logger = LoggerFactory.getLogger(NamespaceController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    @PUT
    @Path("/repositories/{repId}/namespaces/{prefix}")
    public void setNamespace(@Context UriInfo uriInfo, @PathParam("repId") String repId, @PathParam("prefix") String prefix, @QueryParam("context") String[] context, @QueryParam("baseURI") IRI baseURI, String body)
            throws WebApplicationException, IOException {
        if (body.length() == 0) {
            throw new WebApplicationException("Cannot set namespace. Body is empty", BAD_REQUEST);
        } else {
            Repository repository = repositoryManager.getRepository(repId);
            if (repository == null)
                throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

            RepositoryConnection repositoryCon = repository.getConnection();
            // TODO: Проверить контекст
            // ValueFactory vf = repository.getValueFactory();
            // Resource[] contexts = Protocol.decodeContexts(context, vf);

            try {
                if (repositoryCon.getNamespace(prefix) != null) {
                    repositoryCon.setNamespace(prefix, body);
                    logger.info("Updated prefix {} for namespace '{}'", prefix, repositoryCon.getNamespace(prefix));
                } else {
                    repositoryCon.setNamespace(prefix, body);
                    logger.info("Added new prefix {} for namespace '{}'", prefix, repositoryCon.getNamespace(prefix));
                }
            } catch (Exception e) {
                logger.error("Cannot set namespace for " + prefix + " : " + body, e);
                throw new WebApplicationException("Cannot set namespace. Internal error", INTERNAL_SERVER_ERROR);
            }
        }
    }

    @DELETE
    @Path("/repositories/{repId}/namespaces/{prefix}")
    public void deleteNamespace(@PathParam("repId") String repId, @PathParam("prefix") String prefix) throws IOException {
        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository);

        try {
            if (repositoryCon.getNamespace(prefix) != null) {
                repositoryCon.removeNamespace(prefix);
                logger.info("Prefix {} deleted", prefix);
            } else {
                logger.info("Cannot delete, prefix {} does not exist", prefix);
            }
        } catch (Exception e) {
            logger.error("Cannot delete namespace " + prefix + ". Internal error", e);
            throw new WebApplicationException("Cannot delete namespace " + prefix + ". Internal error", INTERNAL_SERVER_ERROR);
        }
    }
}
