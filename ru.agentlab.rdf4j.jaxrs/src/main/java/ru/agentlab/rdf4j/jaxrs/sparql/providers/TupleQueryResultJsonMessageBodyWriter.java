package ru.agentlab.rdf4j.jaxrs.sparql.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.regex.Pattern;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageBodyWirter for <code>TupleQueryResult</code>.
 * Resulting output conforms to:
 * http://www.w3.org/TR/2007/NOTE-rdf-sparql-json-res-20070618/
 * 
 */
@Component(service = MessageBodyWriter.class, property={"osgi.jaxrs.extension=true"})
@Provider
//@JaxrsExtension
@Produces({"application/json", "application/sparql-results+json"})
public class TupleQueryResultJsonMessageBodyWriter implements MessageBodyWriter<TupleQueryResultModel> {
	private final Logger logger = LoggerFactory.getLogger(TupleQueryResultJsonMessageBodyWriter.class);
	
	protected static final String DEFAULT_JSONP_CALLBACK_PARAMETER = "callback";

	protected static final Pattern JSONP_VALIDATOR = Pattern.compile("^[A-Za-z]\\w+$");
	
	@Context
	Request request;
	
	//public TupleQueryResultJsonMessageBodyWriter() {
	//	System.out.println("Init " + this.getClass().getSimpleName());
	//}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return TupleQueryResultModel.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(TupleQueryResultModel t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(TupleQueryResultModel queryResultModel, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,
			Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
		
		try {
			TupleQueryResultWriter qrWriter = new SPARQLResultsJSONWriter(entityStream);
			TupleQueryResult tupleQueryResult = (TupleQueryResult) queryResultModel.get("queryResult");
			/*if (qrWriter.getSupportedSettings().contains(BasicQueryWriterSettings.JSONP_CALLBACK)) {
				String parameter = request.getParameter(DEFAULT_JSONP_CALLBACK_PARAMETER);
	
				if (parameter != null) {
					parameter = parameter.trim();
	
					if (parameter.isEmpty()) {
						parameter = BasicQueryWriterSettings.JSONP_CALLBACK.getDefaultValue();
					}
	
					// check callback function name is a valid javascript function
					// name
					if (!JSONP_VALIDATOR.matcher(parameter).matches()) {
						throw new IOException("Callback function name was invalid");
					}
	
					qrWriter.getWriterConfig().set(BasicQueryWriterSettings.JSONP_CALLBACK, parameter);
				}
			}*/
			
			QueryResults.report(tupleQueryResult, qrWriter);
		} catch (QueryInterruptedException e) {
			logger.error("Query interrupted", e);
			throw new WebApplicationException("Query evaluation took too long", Response.Status.SERVICE_UNAVAILABLE);
		} catch (QueryEvaluationException e) {
			logger.error("Query evaluation error", e);
			throw new WebApplicationException("Query evaluation error: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		} catch (TupleQueryResultHandlerException e) {
			logger.error("Serialization error", e);
			throw new WebApplicationException("Serialization error: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}  finally {
			RepositoryConnection conn = (RepositoryConnection) queryResultModel.get("connection");
			if (conn != null) {
				conn.close();
			}
		}
	}
}
