package ru.agentlab.rdf4j.jaxrs.repository.statements;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.eclipse.rdf4j.http.protocol.Protocol.BASEURI_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.CONTEXT_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INCLUDE_INFERRED_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.INSERT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_LANGUAGE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.REMOVE_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.USING_NAMED_GRAPH_PARAM_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.protocol.transaction.TransactionReader;
import org.eclipse.rdf4j.http.protocol.transaction.operations.TransactionOperation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ru.agentlab.rdf4j.jaxrs.HTTPException;
import ru.agentlab.rdf4j.jaxrs.ProtocolUtil;
import ru.agentlab.rdf4j.jaxrs.repository.ProtocolUtils;
import ru.agentlab.rdf4j.jaxrs.sparql.providers.StatementsResultModel;
import ru.agentlab.rdf4j.jaxrs.util.HttpServerUtil;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service=StatementsController.class, property={"osgi.jaxrs.resource=true"})
public class StatementsController {
	private static final Logger logger = LoggerFactory.getLogger(StatementsController.class);

	@Reference
	private RepositoryManagerComponent repositoryManager;

	@GET
	@Path("/repositories/{repId}/statements")
	public StatementsResultModel getStatements(@PathParam("repId") String repId,
			@QueryParam("subj") String subjStr,
			@QueryParam("pred") String predStr,
			@QueryParam("obj") String objStr,
			@QueryParam("infer") @DefaultValue("true") boolean useInferencing,
			@QueryParam("context") String[] contextsStr) {
		logger.info("GET statements");
		Repository repository = repositoryManager.getRepository(repId);
		ValueFactory vf = repository.getValueFactory();

		Resource subj = Protocol.decodeResource(subjStr, vf);
		IRI pred = Protocol.decodeURI(predStr, vf);
		Value obj = Protocol.decodeValue(objStr, vf);
		Resource[] contexts = Protocol.decodeContexts(contextsStr, vf);

		try {
		    StatementsResultModel model = new StatementsResultModel();
		    model.setConn(repository.getConnection());
		    model.setSubj(subj);
		    model.setPred(pred);
		    model.setObj(obj);
		    model.setContexts(contexts);
			return model;
		} catch (RDFHandlerException e) {
			throw new WebApplicationException("Serialization error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
		} catch (RepositoryException e) {
			throw new WebApplicationException("Repository error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
		}
	}

	@PUT
	@Path("/repositories/{repId}/statements")
	public void replaceStatements(@Context HttpServletRequest request,
			@PathParam("repId") String repId) throws RepositoryException, IOException, HTTPException {
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);

		getAddDataResult(repository, request, true);
	}

	@POST
	@Path("/repositories/{repId}/statements")
	public void addStatements(@Context HttpServletRequest request,
			@PathParam("repId") String repId) throws RepositoryException, IOException, HTTPException {
		logger.info("POST data to repository");
		//logger.info("repId={}, queryLn={}, baseURI={}, infer={}, timeout={}, distinct={}, limit={}, offset={}", repId, queryLnStr, includeInferred, maxQueryTime);
		
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);

		String mimeType = HttpServerUtil.getMIMEType(request.getContentType());
		
		Map<String, String[]>  parms = request.getParameterMap();
		boolean bb = parms.containsKey(Protocol.UPDATE_PARAM_NAME);
		String[] ss = parms.get(Protocol.UPDATE_PARAM_NAME);

		if (Protocol.TXN_MIME_TYPE.equals(mimeType)) {
			logger.info("POST transaction to repository");
			getTransactionResultResult(repository, request);
		} else if (Protocol.SPARQL_UPDATE_MIME_TYPE.equals(mimeType)
		        || request.getParameterMap().containsKey(Protocol.UPDATE_PARAM_NAME)) {
			logger.info("POST SPARQL update request to repository");
			getSparqlUpdateResult(repository, request, null);
		} else {
			logger.info("POST data to repository");
			getAddDataResult(repository, request, false);
		}
	}

	@POST
	@Path("/repositories/{repId}/statements")
	@Consumes ({"application/x-www-form-urlencoded"})
	public void addStatements(@Context HttpServletRequest request,
			@PathParam("repId") String repId,
			@FormParam("update") String queryUpdate) throws RepositoryException, IOException, HTTPException {
		logger.info("POST data to repository");
		//logger.info("repId={}, queryLn={}, baseURI={}, infer={}, timeout={}, distinct={}, limit={}, offset={}", repId, queryLnStr, includeInferred, maxQueryTime);
		
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);

		String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

		int qryCode = 0;
		if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
			qryCode = String.valueOf(queryUpdate).hashCode();
		}

		if (Protocol.TXN_MIME_TYPE.equals(mimeType)) {
			logger.info("POST transaction to repository");
			getTransactionResultResult(repository, request);
		} else if (Protocol.SPARQL_UPDATE_MIME_TYPE.equals(mimeType) || queryUpdate != null) {
			logger.info("POST SPARQL update request to repository");
			getSparqlUpdateResult(repository, request, queryUpdate);
		} else {
            logger.info("POST data to repository");
            getAddDataResult(repository, request, false);
        }
	}

	private void getSparqlUpdateResult(Repository repository, HttpServletRequest request, String queryUpdate)
			throws RepositoryException, IOException, HTTPException {
		//ProtocolUtil.logRequestParameters(request);
	    
	    String mimeType = HttpServerUtil.getMIMEType(request.getContentType());

        String sparqlUpdateString;
        if (Protocol.SPARQL_UPDATE_MIME_TYPE.equals(mimeType)) {
            // The query should be the entire body
            try {
                sparqlUpdateString = IOUtils.toString(request.getReader());
            } catch (IOException e) {
                throw new WebApplicationException("Error reading request message body", e, BAD_REQUEST);
            }
            if (sparqlUpdateString.isEmpty())
                sparqlUpdateString = null;
        } else {
            sparqlUpdateString = queryUpdate;//request.getParameterValues(Protocol.UPDATE_PARAM_NAME)[0];
        }

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

		SimpleDataset dataset = null;
		if (defaultRemoveGraphURIs != null || defaultInsertGraphURIs != null || defaultGraphURIs != null
				|| namedGraphURIs != null) {
			dataset = new SimpleDataset();
		}

		if (defaultRemoveGraphURIs != null) {
			for (String graphURI : defaultRemoveGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, graphURI);
					dataset.addDefaultRemoveGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new WebApplicationException("Illegal URI for default remove graph: " + graphURI, BAD_REQUEST);
				}
			}
		}

		if (defaultInsertGraphURIs != null && defaultInsertGraphURIs.length > 0) {
			String graphURI = defaultInsertGraphURIs[0];
			try {
				IRI uri = createURIOrNull(repository, graphURI);
				dataset.setDefaultInsertGraph(uri);
			} catch (IllegalArgumentException e) {
				throw new WebApplicationException("Illegal URI for default insert graph: " + graphURI, BAD_REQUEST);
			}
		}

		if (defaultGraphURIs != null) {
			for (String defaultGraphURI : defaultGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, defaultGraphURI);
					dataset.addDefaultGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new WebApplicationException("Illegal URI for default graph: " + defaultGraphURI, BAD_REQUEST);
				}
			}
		}

		if (namedGraphURIs != null) {
			for (String namedGraphURI : namedGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, namedGraphURI);
					dataset.addNamedGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new WebApplicationException("Illegal URI for named graph: " + namedGraphURI, BAD_REQUEST);
				}
			}
		}

		final int maxQueryTime = ProtocolUtil.parseTimeoutParam(request);
		try (RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository)) {
			Update update = repositoryCon.prepareUpdate(queryLn, sparqlUpdateString, baseURI);

			update.setIncludeInferred(includeInferred);
			update.setMaxExecutionTime(maxQueryTime);

			if (dataset != null) {
				update.setDataset(dataset);
			}

			// determine if any variable bindings have been set on this
			// update.
			@SuppressWarnings("unchecked")
			Enumeration<String> parameterNames = request.getParameterNames();

			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();

				if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
					String bindingName = parameterName.substring(BINDING_PREFIX.length());
					Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
							repository.getValueFactory());
					update.setBinding(bindingName, bindingValue);
				}
			}

			update.execute();
		} catch (QueryInterruptedException e) {
			throw new WebApplicationException("update execution took too long", SERVICE_UNAVAILABLE);
		} catch (UpdateExecutionException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new WebApplicationException("Repository update error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
			}
		} catch (RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new WebApplicationException("Repository update error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
			}
		} catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
		}
	}
	
    private IRI createURIOrNull(Repository repository, String graphURI) {
        if ("null".equals(graphURI))
            return null;
        return repository.getValueFactory().createIRI(graphURI);
    }

    /**
     * Process several actions as a transaction.
     */
    private void getTransactionResultResult(Repository repository, HttpServletRequest request) throws IOException, WebApplicationException, HTTPException {
        InputStream in = request.getInputStream();
        try (RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository)) {
            logger.debug("Processing transaction...");

            TransactionReader reader = new TransactionReader();
            Iterable<? extends TransactionOperation> txn = reader.parse(in);

            repositoryCon.begin();

            for (TransactionOperation op : txn) {
                op.execute(repositoryCon);
            }

            repositoryCon.commit();
            logger.debug("Transaction processed ");
        } catch (SAXParseException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_DATA, e.getMessage());
            throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
        } catch (SAXException e) {
            throw new WebApplicationException("Failed to parse transaction data: " + e.getMessage(), e, BAD_REQUEST);
        } catch (IOException e) {
            throw new WebApplicationException("Failed to read data: " + e.getMessage(), e, BAD_REQUEST);
        } catch (RepositoryException e) {
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                // custom signal from the backend, throw as HTTPException
                // directly
                // (see SES-1016).
                throw (HTTPException) e.getCause();
            } else {
                throw new WebApplicationException("Repository update error: " + e.getMessage(), e, BAD_REQUEST);
            }
        }
    }
    
	/**
	 * Upload data to the repository.
	 */
	private void getAddDataResult(Repository repository, HttpServletRequest request, boolean replaceCurrent)
	        throws RepositoryException, IOException, HTTPException {
	    String mimeType = HttpServerUtil.getMIMEType(request.getContentType());
	    
		RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mimeType)
			.orElseThrow(
				() -> new WebApplicationException("Unsupported MIME type: " + mimeType, UNSUPPORTED_MEDIA_TYPE));

		ValueFactory vf = repository.getValueFactory();

		Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
		IRI baseURI = ProtocolUtil.parseURIParam(request, BASEURI_PARAM_NAME, vf);
		final boolean preserveNodeIds = ProtocolUtil.parseBooleanParam(request, Protocol.PRESERVE_BNODE_ID_PARAM_NAME,
                false);

		if (baseURI == null) {
			baseURI = vf.createIRI("foo:bar");
			logger.info("no base URI specified, using dummy '{}'", baseURI);
		}

		InputStream in = request.getInputStream();
		try (RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository)) {
			repositoryCon.begin();

			if (preserveNodeIds) {
				repositoryCon.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			}

			if (replaceCurrent) {
				repositoryCon.clear(contexts);
			}
			repositoryCon.add(in, baseURI.toString(), rdfFormat, contexts);

			repositoryCon.commit();
		} catch (UnsupportedRDFormatException e) {
			throw new WebApplicationException("No RDF parser available for format " + rdfFormat.getName(),
				UNSUPPORTED_MEDIA_TYPE);
		} catch (RDFParseException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_DATA, e.getMessage());
			throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
		} catch (IOException e) {
			throw new WebApplicationException("Failed to read data: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
		} catch (RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new WebApplicationException("Repository update error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	@DELETE
    @Path("/repositories/{repId}/statements")
	public void deleteStatements(@Context HttpHeaders headers,
            @PathParam("repId") String repId,
            @QueryParam("context") String[] contextsStr,
            @QueryParam("subj") String subjStr,
            @QueryParam("pred") String predStr,
            @QueryParam("obj") String objStr) throws RepositoryException, IOException, HTTPException {
	    logger.info("DELETE data from repository");
	    
	    Repository repository = repositoryManager.getRepository(repId);
	    if(repository == null)
            throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);
	    
        ValueFactory vf = repository.getValueFactory();

        Resource subj = Protocol.decodeResource(subjStr, vf);
        IRI pred = Protocol.decodeURI(predStr, vf);
        Value obj = Protocol.decodeValue(objStr, vf);
        Resource[] contexts = Protocol.decodeContexts(contextsStr, vf);

        try (RepositoryConnection repositoryCon = repository.getConnection()) {
            repositoryCon.remove(subj, pred, obj, contexts);
        } catch (RepositoryException e) {
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                // custom signal from the backend, throw as HTTPException
                // directly
                // (see SES-1016).
                throw (HTTPException) e.getCause();
            } else {
                throw new WebApplicationException("Repository update error: " + e.getMessage(), e);
            }
        }
	}
}
