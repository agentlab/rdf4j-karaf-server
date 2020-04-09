/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package ru.agentlab.rdf4j.jaxrs.repository;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.SystemRepository;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.HTTPException;
import ru.agentlab.rdf4j.jaxrs.sparql.providers.TupleQueryResultModel;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

/**
 * Handles queries and admin (delete) operations on a repository and renders the
 * results in a format suitable to the type of operation.
 *
 */
@Component(service=RepositoryController.class, property={"osgi.jaxrs.resource=true"})
public class RepositoryController {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryController.class);

	@Reference
	private RepositoryManagerComponent repositoryManager;
	
	/**
	 * TODO: HEAD method???
	 */
	@GET
	@Path("/repositories/{repId}")
	@Produces({ "application/json", "application/sparql-results+json" })
	public TupleQueryResultModel get(@Context UriInfo uriInfo,
			@PathParam("repId") String repId,
			@QueryParam("query") String queryStr,
			@QueryParam("queryLn") String queryLnStr,
			@QueryParam("baseURI") String baseURI,
			@QueryParam("infer") @DefaultValue("true") boolean includeInferred,
			@QueryParam("timeout") int maxQueryTime,
			@QueryParam("distinct") @DefaultValue("false") boolean distinct,
			@QueryParam("limit") @DefaultValue("0") long limit,
			@QueryParam("offset") @DefaultValue("0") long offset) throws WebApplicationException, HTTPException, IOException {
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

		int qryCode = 0;
		if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
			qryCode = String.valueOf(queryStr).hashCode();
		}
		boolean headersOnly = false;
		logger.info("GET query {}", qryCode);
		logger.info("query {} = {}", qryCode, queryStr);
		logger.info("repId={}, queryLn={}, baseURI={}, infer={}, timeout={}, distinct={}, limit={}, offset={}", repId, queryLnStr, baseURI, includeInferred, maxQueryTime, distinct, limit, offset);

		if (queryStr != null) {
			RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository);
			if(repositoryCon == null)
				throw new WebApplicationException("Cannot connect to repository with id=" + repId, INTERNAL_SERVER_ERROR);

			try {
				Query query = getQuery(repository, repositoryCon,
						queryStr, queryLnStr, baseURI, includeInferred, maxQueryTime);

				Object queryResult = null;
				//FileFormatServiceRegistry<? extends FileFormat, ?> registry;

				try {
					if (query instanceof TupleQuery) {
						if (!headersOnly) {
							TupleQuery tQuery = (TupleQuery) query;
							final TupleQueryResult tqr = distinct ? QueryResults.distinctResults(tQuery.evaluate())
									: tQuery.evaluate();
							queryResult = QueryResults.limitResults(tqr, limit, offset);
						}
						//registry = TupleQueryResultWriterRegistry.getInstance();
					} else if (query instanceof GraphQuery) {
						if (!headersOnly) {
							GraphQuery gQuery = (GraphQuery) query;
							final GraphQueryResult qqr = distinct ? QueryResults.distinctResults(gQuery.evaluate())
									: gQuery.evaluate();
							queryResult = QueryResults.limitResults(qqr, limit, offset);
						}
						//registry = RDFWriterRegistry.getInstance();

					} else if (query instanceof BooleanQuery) {
						BooleanQuery bQuery = (BooleanQuery) query;

						queryResult = headersOnly ? null : bQuery.evaluate();
						//registry = BooleanQueryResultWriterRegistry.getInstance();
					} else {
						throw new WebApplicationException("Unsupported query type: "+query.getClass().getName(), BAD_REQUEST);
					}
				} catch (QueryInterruptedException e) {
					logger.info("Query interrupted", e);
					throw new WebApplicationException("Query evaluation took too long", SERVICE_UNAVAILABLE);
				} catch (QueryEvaluationException e) {
					logger.info("Query evaluation error", e);
					if (e.getCause() != null && e.getCause() instanceof HTTPException) {
						// custom signal from the backend, throw as HTTPException
						// directly (see SES-1016).
						throw (HTTPException) e.getCause();
					} else {
						throw new WebApplicationException("Query evaluation error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
					}
				}
				TupleQueryResultModel queryResultModel = new TupleQueryResultModel();
				queryResultModel.put("queryResult", queryResult);
				queryResultModel.put("connection", repositoryCon);
				return queryResultModel;
			} catch (Exception e) {
				// only close the connection when an exception occurs. Otherwise, the QueryResultView will take care of
				// closing it.
				repositoryCon.close();
				throw e;
			}
		} else {
			throw new WebApplicationException("Missing parameter: " + Protocol.QUERY_PARAM_NAME, BAD_REQUEST);
		}
	}

	@POST
	@Path("/repositories/{repId}")
	@Produces({ "application/json", "application/sparql-results+json" })
	@Consumes(Protocol.SPARQL_QUERY_MIME_TYPE)
	public TupleQueryResultModel createSparql(@Context UriInfo uriInfo,
			@PathParam("repId") String repId,
			String queryStr,
			@QueryParam("queryLn") String queryLnStr,
			@QueryParam("baseURI") String baseURI,
			@QueryParam("infer") @DefaultValue("true") boolean includeInferred,
			@QueryParam("timeout") int maxQueryTime,
			@QueryParam("distinct") @DefaultValue("false") boolean distinct,
			@QueryParam("limit") @DefaultValue("0") long limit,
			@QueryParam("offset") @DefaultValue("0") long offset) throws WebApplicationException, HTTPException, IOException {
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

		int qryCode = 0;
		if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
			qryCode = String.valueOf(queryStr).hashCode();
		}
		boolean headersOnly = false;
		logger.info("POST query {}", qryCode);
		logger.info("query {} = {}", qryCode, queryStr);

		if (queryStr != null) {
			RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository);
			if(repositoryCon == null)
				throw new WebApplicationException("Cannot connect to repository with id="+repId, INTERNAL_SERVER_ERROR);
			try {
				Query query = getQuery(repository, repositoryCon,
						queryStr, queryLnStr, baseURI, includeInferred, maxQueryTime);

				Object queryResult = null;
				//FileFormatServiceRegistry<? extends FileFormat, ?> registry;

				try {
					if (query instanceof TupleQuery) {
						if (!headersOnly) {
							TupleQuery tQuery = (TupleQuery) query;
							final TupleQueryResult tqr = distinct ? QueryResults.distinctResults(tQuery.evaluate())
									: tQuery.evaluate();
							queryResult = QueryResults.limitResults(tqr, limit, offset);
						}
						//registry = TupleQueryResultWriterRegistry.getInstance();
					} else if (query instanceof GraphQuery) {
						if (!headersOnly) {
							GraphQuery gQuery = (GraphQuery) query;
							final GraphQueryResult qqr = distinct ? QueryResults.distinctResults(gQuery.evaluate())
									: gQuery.evaluate();
							queryResult = QueryResults.limitResults(qqr, limit, offset);
						}
						//registry = RDFWriterRegistry.getInstance();

					} else if (query instanceof BooleanQuery) {
						BooleanQuery bQuery = (BooleanQuery) query;

						queryResult = headersOnly ? null : bQuery.evaluate();
						//registry = BooleanQueryResultWriterRegistry.getInstance();
					} else {
						throw new WebApplicationException("Unsupported query type: "+query.getClass().getName(), BAD_REQUEST);
					}
				} catch (QueryInterruptedException e) {
					logger.info("Query interrupted", e);
					throw new WebApplicationException("Query evaluation took too long", SERVICE_UNAVAILABLE);
				} catch (QueryEvaluationException e) {
					logger.info("Query evaluation error", e);
					if (e.getCause() != null && e.getCause() instanceof HTTPException) {
						// custom signal from the backend, throw as HTTPException
						// directly (see SES-1016).
						throw (HTTPException) e.getCause();
					} else {
						throw new WebApplicationException("Query evaluation error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
					}
				}
				TupleQueryResultModel queryResultModel = new TupleQueryResultModel();
				queryResultModel.put("queryResult", queryResult);
				queryResultModel.put("connection", repositoryCon);
				return queryResultModel;
			} catch (Exception e) {
				// only close the connection when an exception occurs. Otherwise, the QueryResultView will take care of
				// closing it.
				repositoryCon.close();
				throw e;
			}
		} else {
			throw new WebApplicationException("Missing parameter: "+Protocol.QUERY_PARAM_NAME, BAD_REQUEST);
		}
	}
	
	@POST
    @Path("/repositories/{repId}")
    @Produces({ "application/json", "application/sparql-results+json" })
    @Consumes(Protocol.FORM_MIME_TYPE)
    public TupleQueryResultModel createSparqlForm(@Context UriInfo uriInfo,
            @PathParam("repId") String repId,
            @FormParam("query") String queryStr,
            @FormParam("queryLn") String queryLnStr,
            @FormParam("baseURI") String baseURI,
            @FormParam("infer") @DefaultValue("true") boolean includeInferred,
            @FormParam("timeout") int maxQueryTime,
            @FormParam("distinct") @DefaultValue("false") boolean distinct,
            @FormParam("limit") @DefaultValue("0") long limit,
            @FormParam("offset") @DefaultValue("0") long offset) throws WebApplicationException, HTTPException, IOException {
        Repository repository = repositoryManager.getRepository(repId);
        if(repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        int qryCode = 0;
        if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
            qryCode = String.valueOf(queryStr).hashCode();
        }
        boolean headersOnly = false;
        logger.info("POST query {}", qryCode);
        logger.info("query {} = {}", qryCode, queryStr);

        if (queryStr != null) {
            RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository);
            if(repositoryCon == null)
                throw new WebApplicationException("Cannot connect to repository with id="+repId, INTERNAL_SERVER_ERROR);
            try {
                Query query = getQuery(repository, repositoryCon,
                        queryStr, queryLnStr, baseURI, includeInferred, maxQueryTime);

                Object queryResult = null;
                //FileFormatServiceRegistry<? extends FileFormat, ?> registry;

                try {
                    if (query instanceof TupleQuery) {
                        if (!headersOnly) {
                            TupleQuery tQuery = (TupleQuery) query;
                            final TupleQueryResult tqr = distinct ? QueryResults.distinctResults(tQuery.evaluate())
                                    : tQuery.evaluate();
                            queryResult = QueryResults.limitResults(tqr, limit, offset);
                        }
                        //registry = TupleQueryResultWriterRegistry.getInstance();
                    } else if (query instanceof GraphQuery) {
                        if (!headersOnly) {
                            GraphQuery gQuery = (GraphQuery) query;
                            final GraphQueryResult qqr = distinct ? QueryResults.distinctResults(gQuery.evaluate())
                                    : gQuery.evaluate();
                            queryResult = QueryResults.limitResults(qqr, limit, offset);
                        }
                        //registry = RDFWriterRegistry.getInstance();

                    } else if (query instanceof BooleanQuery) {
                        BooleanQuery bQuery = (BooleanQuery) query;

                        queryResult = headersOnly ? null : bQuery.evaluate();
                        //registry = BooleanQueryResultWriterRegistry.getInstance();
                    } else {
                        throw new WebApplicationException("Unsupported query type: "+query.getClass().getName(), BAD_REQUEST);
                    }
                } catch (QueryInterruptedException e) {
                    logger.info("Query interrupted", e);
                    throw new WebApplicationException("Query evaluation took too long", SERVICE_UNAVAILABLE);
                } catch (QueryEvaluationException e) {
                    logger.info("Query evaluation error", e);
                    if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                        // custom signal from the backend, throw as HTTPException
                        // directly (see SES-1016).
                        throw (HTTPException) e.getCause();
                    } else {
                        throw new WebApplicationException("Query evaluation error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
                    }
                }
                TupleQueryResultModel queryResultModel = new TupleQueryResultModel();
                queryResultModel.put("queryResult", queryResult);
                queryResultModel.put("connection", repositoryCon);
                return queryResultModel;
            } catch (Exception e) {
                // only close the connection when an exception occurs. Otherwise, the QueryResultView will take care of
                // closing it.
                repositoryCon.close();
                throw e;
            }
        } else {
            throw new WebApplicationException("Missing parameter: "+Protocol.QUERY_PARAM_NAME, BAD_REQUEST);
        }
    }

	private Query getQuery(Repository repository, RepositoryConnection repositoryCon,
			String queryStr, String queryLnStr, String baseURI, boolean includeInferred, int maxQueryTime
			) throws WebApplicationException {
		Query result = null;

		// default query language is SPARQL
		QueryLanguage queryLn = QueryLanguage.SPARQL;

		logger.debug("query language param = {}", queryLnStr);

		if (queryLnStr != null) {
			queryLn = QueryLanguage.valueOf(queryLnStr);

			if (queryLn == null) {
				throw new WebApplicationException("Unknown query language: "+queryLnStr, BAD_REQUEST);
			}
		}

		// build a dataset, if specified
		/*String[] defaultGraphURIs = request.getParameterValues(DEFAULT_GRAPH_PARAM_NAME);
		String[] namedGraphURIs = request.getParameterValues(NAMED_GRAPH_PARAM_NAME);*/

		SimpleDataset dataset = null;
		/*if (defaultGraphURIs != null || namedGraphURIs != null) {
			dataset = new SimpleDataset();

			if (defaultGraphURIs != null) {
				for (String defaultGraphURI : defaultGraphURIs) {
					try {
						IRI uri = createURIOrNull(repository, defaultGraphURI);
						dataset.addDefaultGraph(uri);
					} catch (IllegalArgumentException e) {
						throw new WebApplicationException("Illegal URI for default graph: "+defaultGraphURI, BAD_REQUEST);
					}
				}
			}

			if (namedGraphURIs != null) {
				for (String namedGraphURI : namedGraphURIs) {
					try {
						IRI uri = createURIOrNull(repository, namedGraphURI);
						dataset.addNamedGraph(uri);
					} catch (IllegalArgumentException e) {
						throw new WebApplicationException("Illegal URI for named graph: "+namedGraphURI, BAD_REQUEST);
					}
				}
			}
		}*/

		try {
			result = repositoryCon.prepareQuery(queryLn, queryStr, baseURI);

			result.setIncludeInferred(includeInferred);

			if (maxQueryTime > 0) {
				result.setMaxQueryTime(maxQueryTime);
			}

			if (dataset != null) {
				result.setDataset(dataset);
			}

			// determine if any variable bindings have been set on this query.
			/*@SuppressWarnings("unchecked")
			Enumeration<String> parameterNames = request.getParameterNames();

			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();

				if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
					String bindingName = parameterName.substring(BINDING_PREFIX.length());
					Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
							repository.getValueFactory());
					result.setBinding(bindingName, bindingValue);
				}
			}*/
		} catch (UnsupportedQueryLanguageException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_QUERY_LANGUAGE, queryLn.getName());
			throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
		} catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
		} catch (RepositoryException e) {
			logger.error("Repository error", e);
			throw new WebApplicationException("Repository error", INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	private IRI createURIOrNull(Repository repository, String graphURI) {
		if ("null".equals(graphURI))
			return null;
		return repository.getValueFactory().createIRI(graphURI);
	}

	private static QueryResult<?> distinct(QueryResult<?> qr) {
		if (qr instanceof TupleQueryResult) {
			TupleQueryResult tqr = (TupleQueryResult) qr;
			return QueryResults.distinctResults(tqr);
		} else if (qr instanceof GraphQueryResult) {
			GraphQueryResult gqr = (GraphQueryResult) qr;
			return QueryResults.distinctResults(gqr);
		} else {
			return qr;
		}
	}

	@PUT
	@Path("/repositories/{repId}")
	public void createRep(InputStream requestBody, @PathParam("repId") String repId, @Context HttpHeaders headers)
			throws WebApplicationException, UnsupportedRDFormatException, IOException {
		logger.info("PUT request invoked for repository '{}'", repId);
		try {
			Model model = Rio.parse(requestBody, "",
				Rio.getParserFormatForMIMEType(headers.getHeaderString(HttpHeaders.CONTENT_TYPE))
					.orElseThrow(() -> new WebApplicationException(
							"unrecognized content type " + headers.getHeaderString(HttpHeaders.CONTENT_TYPE), BAD_REQUEST)));
			RepositoryConfig config = RepositoryConfigUtil.getRepositoryConfig(model, repId);
			repositoryManager.addRepositoryConfig(config);
		} catch (RDF4JException e) {
			logger.error("error while attempting to create/configure repository '" + repId + "'", e);
			throw new WebApplicationException("Repository create error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
		}
	}

	@DELETE
	@Path("/repositories/{repId}")
	public void delete(@PathParam("repId") String repId, @QueryParam("query") String query) throws WebApplicationException {
		logger.info("DELETE request invoked for repository '{}'", repId);

		if (query != null) {
			logger.warn("query supplied on repository delete request, aborting delete");
			throw new WebApplicationException("Repository delete error: query supplied with request", BAD_REQUEST);
		}

		if (SystemRepository.ID.equals(repId)) {
			logger.warn("attempted delete of SYSTEM repository, aborting");
			throw new WebApplicationException("SYSTEM Repository can not be deleted", FORBIDDEN);
		}

		try {
			boolean success = repositoryManager.removeRepository(repId);
			if (success) {
				logger.info("DELETE request successfully completed");
				return;
			} else {
				logger.error("error while attempting to delete repository '" + repId + "'");
				throw new WebApplicationException(
						"could not locate repository configuration for repository '" + repId + "'.", BAD_REQUEST);
			}
		} catch (RDF4JException e) {
			logger.error("error while attempting to delete repository '" + repId + "'", e);
			throw new WebApplicationException("Repository delete error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
		}
	}
}
