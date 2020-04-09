package ru.agentlab.rdf4j.jaxrs.repository.contexts;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = ContextController.class, property = { "osgi.jaxrs.resource=true" })
//@Path("/rdf4j-server")
public class ContextController {
    //private static final Logger logger = LoggerFactory.getLogger(ContextController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    @GET
    @Path("/repositories/{repid}/context")
    public String get(@PathParam("repid") String repId) {
        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        List<String> columnNames = Arrays.asList("contextID");
        List<BindingSet> contexts = new ArrayList<>();
        RepositoryConnection repositoryCon = repository.getConnection();

        try (CloseableIteration<? extends Resource, RepositoryException> contextIter = repositoryCon.getContextIDs()) {
            while (contextIter.hasNext()) {
                BindingSet bindingSet = new ListBindingSet(columnNames, contextIter.next());
                contexts.add(bindingSet);
            }
        }
        return Arrays.toString(contexts.toArray());
    }
}
