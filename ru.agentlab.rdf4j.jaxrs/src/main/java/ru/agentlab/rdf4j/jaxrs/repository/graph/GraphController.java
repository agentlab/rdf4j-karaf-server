package ru.agentlab.rdf4j.jaxrs.repository.graph;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.ProtocolUtil;
import ru.agentlab.rdf4j.jaxrs.repository.ProtocolUtils;
import ru.agentlab.rdf4j.jaxrs.sparql.providers.StatementsResultModel;
import ru.agentlab.rdf4j.jaxrs.util.HttpServerUtil;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = GraphController.class, property = { "osgi.jaxrs.resource=true" })
//@Path("/rdf4j-server")
public class GraphController {
    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    @GET
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public StatementsResultModel getGraph(@Context HttpServletRequest request, @PathParam("repId") String repId, @PathParam("graphName") String graphName) throws IOException {
        logger.info("GET data to graph, repId = {}, graphName = {}", repId, graphName);

        final Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        ValueFactory vf = repository.getValueFactory();

        IRI graph = getGraphName(request, vf);
        
        StatementsResultModel model = new StatementsResultModel();
        
        model.setConn(repository.getConnection());
        model.setSubj(null);
        model.setPred(null);
        model.setObj(null);
        model.setUseInferencing(true);
        model.setContexts(new Resource[] { graph });
        return model;
    }
    
    private IRI getGraphName(HttpServletRequest request, ValueFactory vf) throws WebApplicationException {
        String requestURL = request.getRequestURL().toString();
        boolean isServiceRequest = requestURL.endsWith("/service");

        String queryString = request.getQueryString();

        if (isServiceRequest) {
            if (!"default".equalsIgnoreCase(queryString)) {
                IRI graph = ProtocolUtil.parseGraphParam(request, vf);
                if (graph == null) {
                    throw new WebApplicationException("Named or default graph expected for indirect reference request.", BAD_REQUEST);
                }
                return graph;
            }
            return null;
        } else {
            if (queryString != null) {
                throw new WebApplicationException("No parameters epxected for direct reference request.", BAD_REQUEST);
            }
            return vf.createIRI(requestURL);
        }
    }

    @POST
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public void postGraphStatements(@Context HttpServletRequest request,
            @PathParam("repId") String repId, @PathParam("graphName") String graphName) throws IOException {
        logger.info("POST data to graph, repId = {}, graphName = {}", repId, graphName);

        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);
        
        getAddDataResult(repository, request, false);
        logger.info("POST data request finished.");
    }
    
    @PUT
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public void putGraphStatements(@Context HttpServletRequest request,
            @PathParam("repId") String repId, @PathParam("graphName") String graphName) throws IOException {
        logger.info("PUT data to graph, repId = {}, graphName = {}", repId, graphName);

        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);
        
        getAddDataResult(repository, request, true);
        logger.info("PUT data request finished.");
    }
    
    /**
     * Upload data to the graph.
     */
    private void getAddDataResult(Repository repository, HttpServletRequest request, boolean replaceCurrent)
            throws IOException, WebApplicationException {
        ProtocolUtil.logRequestParameters(request);

        String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

        RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mimeType)
                .orElseThrow(
                        () -> new WebApplicationException("Unsupported MIME type: " + mimeType, UNSUPPORTED_MEDIA_TYPE));

        ValueFactory vf = repository.getValueFactory();
        final IRI graph = getGraphName(request, vf);

        IRI baseURI = ProtocolUtil.parseURIParam(request, Protocol.BASEURI_PARAM_NAME, vf);
        if (baseURI == null) {
            baseURI = graph != null ? graph : vf.createIRI("foo:bar");
            logger.info("no base URI specified, using '{}'", baseURI);
        }

        InputStream in = request.getInputStream();
        try (RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository)) {
            boolean localTransaction = !repositoryCon.isActive();

            if (localTransaction) {
                repositoryCon.begin();
            }

            if (replaceCurrent) {
                repositoryCon.clear(graph);
            }
            repositoryCon.add(in, baseURI.stringValue(), rdfFormat, graph);

            if (localTransaction) {
                repositoryCon.commit();
            }
        } catch (UnsupportedRDFormatException e) {
            throw new WebApplicationException("No RDF parser available for format " + rdfFormat.getName(), UNSUPPORTED_MEDIA_TYPE);
        } catch (RDFParseException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_DATA, e.getMessage());
            throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
        } catch (IOException e) {
            throw new WebApplicationException("Failed to read data: " + e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new WebApplicationException("Repository update error: " + e.getMessage(), e);
        }
    }

    /**
     * Delete data from the graph.
     */
    @DELETE
    @Path("/repositories/{repId}/rdf-graphs/{graphName}")
    public void deleteGraph(@Context HttpServletRequest request, @PathParam("repId") String repId, @PathParam("graphName") String graphName, @Context UriInfo uri) throws IOException {
        logger.info("DELETE data from graph {} from repository {}", graphName, repId);

        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        ValueFactory vf = repository.getValueFactory();

        IRI graph = getGraphName(request, vf);

        try (RepositoryConnection repositoryCon = repository.getConnection()) {
            repositoryCon.clear(graph);
            logger.info("Graph {} deleted", graphName);
        } catch (Exception e) {
            logger.error("Cannot delete graph " + graphName + ". Internal error", e);
            throw new WebApplicationException("Cannot delete graph " + graphName + ". Internal error", INTERNAL_SERVER_ERROR);
        }
    };
}
