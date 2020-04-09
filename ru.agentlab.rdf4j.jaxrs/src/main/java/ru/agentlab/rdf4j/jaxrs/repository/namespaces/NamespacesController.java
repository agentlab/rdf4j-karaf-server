package ru.agentlab.rdf4j.jaxrs.repository.namespaces;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.ServerHTTPException;
import ru.agentlab.rdf4j.jaxrs.sparql.providers.TupleQueryResultModel;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = NamespacesController.class, property = { "osgi.jaxrs.resource=true" })
public class NamespacesController {
    private static final Logger logger = LoggerFactory.getLogger(NamespacesController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    @GET
    @Path("/repositories/{repId}/namespaces")
    @Produces({ "application/json", "application/sparql-results+json" })
    public TupleQueryResultModel get(@Context UriInfo uriInfo, @PathParam("repId") String repId, @QueryParam("context") Resource[] context) throws RDF4JException, IOException, ServerHTTPException {
        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        RepositoryConnection repositoryCon = repository.getConnection();
        if (repositoryCon == null)
            throw new WebApplicationException("Cannot connect to repository with id=" + repId, INTERNAL_SERVER_ERROR);

        TupleQueryResult queryResult = null;

        try {
            final ValueFactory vf = repositoryCon.getValueFactory();
            List<BindingSet> bindingSets = new ArrayList<>();
            List<String> bindingNames = new ArrayList<>();
            
            repositoryCon.getNamespaces().forEach(ns -> {
                QueryBindingSet bindings = new QueryBindingSet();
                bindings.addBinding("prefix", vf.createLiteral(ns.getPrefix()));
                bindings.addBinding("namespace", vf.createLiteral(ns.getName()));
                bindingSets.add(bindings);
            });
            bindingNames.add("prefix");
            bindingNames.add("namespace");
            queryResult = new IteratingTupleQueryResult(bindingNames, bindingSets);
        } catch (Exception e) {
            logger.error("Namespaces error for repository=" + repId, e);
            throw new WebApplicationException("Repository error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
        }
        TupleQueryResultModel queryResultModel = new TupleQueryResultModel();
        queryResultModel.put("queryResult", queryResult);
        queryResultModel.put("connection", repositoryCon);
        return queryResultModel;
    }

    @DELETE
    @Path("/repositories/{repId}/namespaces")
    @Produces({ "application/json", "application/sparql-results+xml" })
    public void remove(@PathParam("repId") String repId) throws ServerHTTPException {
        Repository repository = repositoryManager.getRepository(repId);
        try (RepositoryConnection repositoryCon = repository.getConnection()) {
            repositoryCon.clearNamespaces();
        } catch (Exception e) {
            throw new WebApplicationException("Repository error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }
}
