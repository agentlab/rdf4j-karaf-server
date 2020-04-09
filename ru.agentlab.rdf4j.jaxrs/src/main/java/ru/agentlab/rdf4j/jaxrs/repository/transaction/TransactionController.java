package ru.agentlab.rdf4j.jaxrs.repository.transaction;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.CONTEXT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.DEFAULT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INSERT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.NAMED_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.OBJECT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.PREDICATE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.REMOVE_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.SUBJECT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_NAMED_GRAPH_PARAM_NAME;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.Action;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.HTTPException;
import ru.agentlab.rdf4j.jaxrs.ProtocolUtil;
import ru.agentlab.rdf4j.jaxrs.sparql.providers.StatementsResultModel;
import ru.agentlab.rdf4j.jaxrs.sparql.providers.TupleQueryResultModel;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = TransactionController.class, property = {"osgi.jaxrs.resource=true"})
//@Path("/rdf4j-server")
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;
    
    @GET
    @Path("/repositories/{repId}/transactions/{txnId}")
    public Object pingTransaction(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("repId") String repId, @PathParam("txnId") String txnId) {
        UUID transactionId = getTransactionID(txnId);
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());
        
        Transaction transaction = ActiveTransactionRegistry.INSTANCE.getTransaction(transactionId);

        if (transaction == null) {
            logger.warn("could not find transaction for transaction id {}", transactionId);
            throw new WebApplicationException("unable to find registered transaction for transaction id '" + transactionId + "'", BAD_REQUEST);
        }

        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        Object result;
        switch (action) {
        case PING:
            result = Long.toString(ActiveTransactionRegistry.INSTANCE.getTimeout(TimeUnit.MILLISECONDS));
            break;
        default:
            throw new WebApplicationException("GET Method not allowed", METHOD_NOT_ALLOWED);
        }
        ActiveTransactionRegistry.INSTANCE.active(transaction);
        return result;
    }

    @PUT
    @Path("/repositories/{repId}/transactions/{txnId}")
    public Object putTransaction(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("repId") String repId, @PathParam("txnId") String txnId) throws Exception {
        UUID transactionId = getTransactionID(txnId);
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());
        
        Transaction transaction = ActiveTransactionRegistry.INSTANCE.getTransaction(transactionId);

        if (transaction == null) {
            logger.warn("could not find transaction for transaction id {}", transactionId);
            throw new WebApplicationException("unable to find registered transaction for transaction id '" + transactionId + "'", BAD_REQUEST);
        }

        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        Object result;
        switch (action) {
        case QUERY:
            // TODO SES-2238 note that we allow POST requests for backward
            // compatibility reasons with earlier
            // 2.8.x releases, even though according to the protocol spec only
            // PUT is allowed.
            logger.info("PUT txn query request");
            result = processQuery(transaction, request);
            logger.info("PUT txn query request finished");
            break;
        case GET:
            logger.info("PUT txn get/export statements request");
            result = getExportStatementsResult(transaction, request);
            logger.info("PUT txn get/export statements request finished");
            break;
        case SIZE:
            logger.info("PUT txn size request");
            result = getSize(repId, transaction, request);
            logger.info("PUT txn size request finished");
            break;
        case PING:
            result = Long.toString(ActiveTransactionRegistry.INSTANCE.getTimeout(TimeUnit.MILLISECONDS));
            break;
        case ROLLBACK:
            logger.info("PUT transaction rollback");
            try {
                transaction.rollback();
            } finally {
                ActiveTransactionRegistry.INSTANCE.deregister(transaction);
            }
            result = Response.noContent().build();
            logger.info("PUT transaction rollback request finished.");
            break;
        default:
            // TODO Action.ROLLBACK check is for backward compatibility with
            // older 2.8.x releases only. It's not in the protocol spec.
            logger.info("PUT txn operation");
            result = processModificationOperation(transaction, action, request);
            logger.info("PUT txn operation request finished.");
            break;
        }
        ActiveTransactionRegistry.INSTANCE.active(transaction);
        return result;
    }
    
    @POST
    @Path("/repositories/{repId}/transactions/{txnId}")
    public Object postTransaction(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("repId") String repId, @PathParam("txnId") String txnId) throws Exception {
        UUID transactionId = getTransactionID(txnId);
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());
        
        Transaction transaction = ActiveTransactionRegistry.INSTANCE.getTransaction(transactionId);

        if (transaction == null) {
            logger.warn("could not find transaction for transaction id {}", transactionId);
            throw new WebApplicationException("unable to find registered transaction for transaction id '" + transactionId + "'", BAD_REQUEST);
        }

        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        Object result;
        switch (action) {
        case QUERY:
            // TODO SES-2238 note that we allow POST requests for backward
            // compatibility reasons with earlier
            // 2.8.x releases, even though according to the protocol spec only
            // PUT is allowed.
            logger.info("POST txn query request");
            result = processQuery(transaction, request);
            logger.info("POST txn query request finished");
            break;
        case GET:
            logger.info("POST txn get/export statements request");
            result = getExportStatementsResult(transaction, request);
            logger.info("POST txn get/export statements request finished");
            break;
        case SIZE:
            logger.info("POST txn size request");
            result = getSize(repId, transaction, request);
            logger.info("POST txn size request finished");
            break;
        case PING:
            result = Long.toString(ActiveTransactionRegistry.INSTANCE.getTimeout(TimeUnit.MILLISECONDS));
            break;
        case ROLLBACK:
            logger.info("POST transaction rollback");
            try {
                transaction.rollback();
            } finally {
                ActiveTransactionRegistry.INSTANCE.deregister(transaction);
            }
            result = Response.noContent().build();
            logger.info("POST transaction rollback request finished.");
            break;
        default:
            // TODO Action.ROLLBACK check is for backward compatibility with
            // older 2.8.x releases only. It's not in the protocol spec.
            logger.info("POST txn operation");
            result = processModificationOperation(transaction, action, request);
            logger.info("POST txn operation request finished.");
            break;
        }
        ActiveTransactionRegistry.INSTANCE.active(transaction);
        return result;
    }
    
    @DELETE
    @Path("/repositories/{repId}/transactions/{txnId}")
    public Object deleteTransaction(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("repId") String repId, @PathParam("txnId") String txnId) throws Exception {
        UUID transactionId = getTransactionID(txnId);
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());
        
        Transaction transaction = ActiveTransactionRegistry.INSTANCE.getTransaction(transactionId);

        if (transaction == null) {
            logger.warn("could not find transaction for transaction id {}", transactionId);
            throw new WebApplicationException("unable to find registered transaction for transaction id '" + transactionId + "'", BAD_REQUEST);
        }

        Object result;
        logger.info("DELETE transaction rollback");
        try {
            transaction.rollback();
        } finally {
            ActiveTransactionRegistry.INSTANCE.deregister(transaction);
        }
        result = Response.noContent().build();
        logger.info("DELETE transaction rollback request finished.");
        
        return result;
    }
    
    private UUID getTransactionID(String txnId) throws WebApplicationException {
        UUID txnID = null;

        try {
            txnID = UUID.fromString(txnId);
            logger.debug("txnID is '{}'", txnID);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("not a valid transaction id: " + txnId, BAD_REQUEST);
        }

        return txnID;
    }
    
    private Object processModificationOperation(Transaction transaction, Action action,
            HttpServletRequest request) throws IOException, HTTPException {
        ProtocolUtil.logRequestParameters(request);

        String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);
        if (baseURI == null) {
            baseURI = "";
        }

        final Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME,
                SimpleValueFactory.getInstance());

        final boolean preserveNodeIds = ProtocolUtil.parseBooleanParam(request, Protocol.PRESERVE_BNODE_ID_PARAM_NAME,
                false);

        try {
            RDFFormat format = null;
            switch (action) {
            case ADD:
                format = Rio.getParserFormatForMIMEType(request.getContentType())
                        .orElseThrow(Rio.unsupportedFormat(request.getContentType()));
                transaction.add(request.getInputStream(), baseURI, format, preserveNodeIds, contexts);
                break;
            case DELETE:
                format = Rio.getParserFormatForMIMEType(request.getContentType())
                        .orElseThrow(Rio.unsupportedFormat(request.getContentType()));
                transaction.delete(format, request.getInputStream(), baseURI);

                break;
            case UPDATE:
                return getSparqlUpdateResult(transaction, request);
            case COMMIT:
                transaction.commit();
                // If commit fails with an exception, deregister should be skipped so the user
                // has a chance to do a proper rollback. See #725.
                ActiveTransactionRegistry.INSTANCE.deregister(transaction);
                break;
            default:
                logger.warn("transaction modification action '{}' not recognized", action);
                throw new WebApplicationException("modification action not recognized: " + action);
            }
            return Response.ok().build();
        } catch (Exception e) {
            if (e instanceof WebApplicationException) {
                throw (WebApplicationException) e;
            } else {
                throw new WebApplicationException("Transaction handling error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
            }
        }
    }
    
    private Response getSize(String repId, Transaction transaction, HttpServletRequest request) throws WebApplicationException, WebApplicationException {
        ProtocolUtil.logRequestParameters(request);

        //String repId = (String) request.getAttribute("repositoryID");
        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        ValueFactory vf = repository.getValueFactory();
        Resource[] contexts = ProtocolUtil.parseContextParam(request, Protocol.CONTEXT_PARAM_NAME, vf);

        long size = -1;

        try {
            size = transaction.getSize(contexts);
        } catch (RepositoryException | InterruptedException | ExecutionException e) {
            throw new WebApplicationException("Repository error: " + e.getMessage(), e);
        }
        return Response.ok(size).build();
    }

    /**
     * Get all statements and export them as RDF.
     * 
     * @return a model and view for exporting the statements.
     */
    private StatementsResultModel getExportStatementsResult(Transaction transaction, HttpServletRequest request) throws WebApplicationException {
        ProtocolUtil.logRequestParameters(request);

        ValueFactory vf = SimpleValueFactory.getInstance();
        
        Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
        IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
        Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
        Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
        boolean useInferencing = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);
        
        StatementsResultModel model = new StatementsResultModel();
        model.setTransaction(transaction);
        model.setSubj(subj);
        model.setPred(pred);
        model.setObj(obj);
        model.setContexts(contexts);
        model.setUseInferencing(useInferencing);
        return model;
    }

    /**
     * Evaluates a query on the given connection and returns the resulting {@link QueryResultView}. The
     * {@link QueryResultView} will take care of correctly releasing the connection back to the
     * {@link ActiveTransactionRegistry}, after fully rendering the query result for sending over the wire.
     */
    private TupleQueryResultModel processQuery(Transaction txn, HttpServletRequest request) throws IOException, WebApplicationException, RepositoryException, InterruptedException {
        String queryStr = null;
        final String contentType = request.getContentType();
        if (contentType != null && contentType.contains(Protocol.SPARQL_QUERY_MIME_TYPE)) {
            final String encoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
            queryStr = IOUtils.toString(request.getInputStream(), encoding);
        } else {
            queryStr = request.getParameter(QUERY_PARAM_NAME);
        }
        
        Object queryResult;
        
        try {
            Query query = getQuery(txn, queryStr, request);
            
            if (query instanceof TupleQuery) {
                TupleQuery tQuery = (TupleQuery) query;
                queryResult = txn.evaluate(tQuery);
            } else if (query instanceof GraphQuery) {
                GraphQuery gQuery = (GraphQuery) query;
                queryResult = txn.evaluate(gQuery);
            } else if (query instanceof BooleanQuery) {
                BooleanQuery bQuery = (BooleanQuery) query;
                queryResult = txn.evaluate(bQuery);
            } else {
                throw new WebApplicationException("Unsupported query type: " + query.getClass().getName(), BAD_REQUEST);
            }
        } catch (QueryInterruptedException | InterruptedException | ExecutionException e) {
            logger.info("Query interrupted", e);
            throw new WebApplicationException("Query evaluation took too long", SERVICE_UNAVAILABLE);
        } catch (QueryEvaluationException e) {
            logger.info("Query evaluation error", e);
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                // custom signal from the backend, throw as HTTPException
                // directly (see SES-1016).
                throw (WebApplicationException) e.getCause();
            } else {
                throw new WebApplicationException("Query evaluation error: " + e.getMessage());
            }
        }
        TupleQueryResultModel model = new TupleQueryResultModel();
        model.put("filenameHint", "query-result");
        model.put("queryResult", queryResult);
        return model;
    }

    private Query getQuery(Transaction txn, String queryStr, HttpServletRequest request) throws IOException, WebApplicationException, InterruptedException, ExecutionException {
        Query result = null;
        
        // default query language is SPARQL
        QueryLanguage queryLn = QueryLanguage.SPARQL;
        
        String queryLnStr = request.getParameter(QUERY_LANGUAGE_PARAM_NAME);
        logger.debug("query language param = {}", queryLnStr);

        if (queryLnStr != null) {
            queryLn = QueryLanguage.valueOf(queryLnStr);
            if (queryLn == null) {
                throw new WebApplicationException("Unknown query language: " + queryLnStr, BAD_REQUEST);
            }
        }
        
        String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);
        
        // determine if inferred triples should be included in query evaluation
        boolean includeInferred = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);
        
        String timeout = request.getParameter(Protocol.TIMEOUT_PARAM_NAME);
        int maxQueryTime = 0;
        if (timeout != null) {
            try {
                maxQueryTime = Integer.parseInt(timeout);
            } catch (NumberFormatException e) {
                throw new WebApplicationException("Invalid timeout value: " + timeout, BAD_REQUEST);
            }
        }

        // build a dataset, if specified
        String[] defaultGraphURIs = request.getParameterValues(DEFAULT_GRAPH_PARAM_NAME);
        String[] namedGraphURIs = request.getParameterValues(NAMED_GRAPH_PARAM_NAME);

        SimpleDataset dataset = null;
        if (defaultGraphURIs != null || namedGraphURIs != null) {
            dataset = new SimpleDataset();

            if (defaultGraphURIs != null) {
                for (String defaultGraphURI : defaultGraphURIs) {
                    try {
                        IRI uri = null;
                        if (!"null".equals(defaultGraphURI)) {
                            uri = SimpleValueFactory.getInstance().createIRI(defaultGraphURI);
                        }
                        dataset.addDefaultGraph(uri);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException("Illegal URI for default graph: " + defaultGraphURI, BAD_REQUEST);
                    }
                }
            }
            if (namedGraphURIs != null) {
                for (String namedGraphURI : namedGraphURIs) {
                    try {
                        IRI uri = null;
                        if (!"null".equals(namedGraphURI)) {
                            uri = SimpleValueFactory.getInstance().createIRI(namedGraphURI);
                        }
                        dataset.addNamedGraph(uri);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException("Illegal URI for named graph: " + namedGraphURI, BAD_REQUEST);
                    }
                }
            }
        }

        try {
            result = txn.prepareQuery(queryLn, queryStr, baseURI);
            result.setIncludeInferred(includeInferred);
            
            if (maxQueryTime > 0) {
                result.setMaxExecutionTime(maxQueryTime);
            }
            
            if (dataset != null) {
                result.setDataset(dataset);
            }

            // determine if any variable bindings have been set on this query.
            @SuppressWarnings("unchecked")
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();

                if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
                    String bindingName = parameterName.substring(BINDING_PREFIX.length());
                    Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName, SimpleValueFactory.getInstance());
                    result.setBinding(bindingName, bindingValue);
                }
            }
        } catch (UnsupportedQueryLanguageException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_QUERY_LANGUAGE, queryLn.getName());
            throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
        } catch (MalformedQueryException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
            throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
        } catch (RepositoryException e) {
            logger.error("Repository error", e);
            throw new WebApplicationException("Repository error" + e, INTERNAL_SERVER_ERROR);
        }
        return result;
    }
    
    private Object getSparqlUpdateResult(Transaction transaction, HttpServletRequest request) throws WebApplicationException, HTTPException {
        String sparqlUpdateString = null;
        final String contentType = request.getContentType();
        if (contentType != null && contentType.contains(Protocol.SPARQL_UPDATE_MIME_TYPE)) {
            try {
                final String encoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding()
                        : "UTF-8";
                sparqlUpdateString = IOUtils.toString(request.getInputStream(), encoding);
            } catch (IOException e) {
                logger.warn("error reading sparql update string from request body", e);
                throw new WebApplicationException("could not read SPARQL update string from body: " + e.getMessage(), BAD_REQUEST);
            }
        } else {
            sparqlUpdateString = request.getParameter(Protocol.UPDATE_PARAM_NAME);
        }

        if (null == sparqlUpdateString) {
            throw new WebApplicationException("Could not read SPARQL update string from body.", NOT_ACCEPTABLE);
        }

        logger.debug("SPARQL update string: {}", sparqlUpdateString);

        // default query language is SPARQL
        QueryLanguage queryLn = QueryLanguage.SPARQL;

        String queryLnStr = request.getParameter(QUERY_LANGUAGE_PARAM_NAME);
        logger.debug("query language param = {}", queryLnStr);

        if (queryLnStr != null) {
            queryLn = QueryLanguage.valueOf(queryLnStr);

            if (queryLn == null) {
                throw new WebApplicationException("Unknown query language: " + queryLnStr, BAD_REQUEST);
            }
        }

        String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);

        // determine if inferred triples should be included in query evaluation
        boolean includeInferred = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

        // build a dataset, if specified
        String[] defaultRemoveGraphURIs = request.getParameterValues(REMOVE_GRAPH_PARAM_NAME);
        String[] defaultInsertGraphURIs = request.getParameterValues(INSERT_GRAPH_PARAM_NAME);
        String[] defaultGraphURIs = request.getParameterValues(USING_GRAPH_PARAM_NAME);
        String[] namedGraphURIs = request.getParameterValues(USING_NAMED_GRAPH_PARAM_NAME);

        SimpleDataset dataset = new SimpleDataset();

        if (defaultRemoveGraphURIs != null) {
            for (String graphURI : defaultRemoveGraphURIs) {
                try {
                    IRI uri = null;
                    if (!"null".equals(graphURI)) {
                        uri = SimpleValueFactory.getInstance().createIRI(graphURI);
                    }
                    dataset.addDefaultRemoveGraph(uri);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException("Illegal URI for default remove graph: " + graphURI, BAD_REQUEST);
                }
            }
        }

        if (defaultInsertGraphURIs != null && defaultInsertGraphURIs.length > 0) {
            String graphURI = defaultInsertGraphURIs[0];
            try {
                IRI uri = null;
                if (!"null".equals(graphURI)) {
                    uri = SimpleValueFactory.getInstance().createIRI(graphURI);
                }
                dataset.setDefaultInsertGraph(uri);
            } catch (IllegalArgumentException e) {
                throw new WebApplicationException("Illegal URI for default insert graph: " + graphURI, BAD_REQUEST);
            }
        }

        if (defaultGraphURIs != null) {
            for (String defaultGraphURI : defaultGraphURIs) {
                try {
                    IRI uri = null;
                    if (!"null".equals(defaultGraphURI)) {
                        uri = SimpleValueFactory.getInstance().createIRI(defaultGraphURI);
                    }
                    dataset.addDefaultGraph(uri);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException("Illegal URI for default graph: " + defaultGraphURI, BAD_REQUEST);
                }
            }
        }

        if (namedGraphURIs != null) {
            for (String namedGraphURI : namedGraphURIs) {
                try {
                    IRI uri = null;
                    if (!"null".equals(namedGraphURI)) {
                        uri = SimpleValueFactory.getInstance().createIRI(namedGraphURI);
                    }
                    dataset.addNamedGraph(uri);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException("Illegal URI for named graph: " + namedGraphURI, BAD_REQUEST);
                }
            }
        }

        try {
            // determine if any variable bindings have been set on this update.
            @SuppressWarnings("unchecked")
            Enumeration<String> parameterNames = request.getParameterNames();

            Map<String, Value> bindings = new HashMap<>();
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();

                if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
                    String bindingName = parameterName.substring(BINDING_PREFIX.length());
                    Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
                            SimpleValueFactory.getInstance());
                    bindings.put(bindingName, bindingValue);
                }
            }

            transaction.executeUpdate(queryLn, sparqlUpdateString, baseURI, includeInferred, dataset, bindings);

            return Response.ok().build();
        } catch (UpdateExecutionException | InterruptedException | ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                // custom signal from the backend, throw as HTTPException directly
                // (see SES-1016).
                throw (HTTPException) e.getCause();
            } else {
                throw new WebApplicationException("Repository update error: " + e.getMessage(), e);
            }
        } catch (RepositoryException e) {
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                // custom signal from the backend, throw as HTTPException directly
                // (see SES-1016).
                throw (HTTPException) e.getCause();
            } else {
                throw new WebApplicationException("Repository update error: " + e.getMessage(), e);
            }
        } catch (MalformedQueryException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
            throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
        }
    }
}