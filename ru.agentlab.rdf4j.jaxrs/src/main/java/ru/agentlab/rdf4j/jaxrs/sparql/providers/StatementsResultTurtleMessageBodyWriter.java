package ru.agentlab.rdf4j.jaxrs.sparql.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

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
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.repository.transaction.Transaction;

/**
 * MessageBodyWirter for <code>StatementsResultModel</code>.
 * 
 */
@Component(service = MessageBodyWriter.class, property={"osgi.jaxrs.extension=true"})
@Provider
//@JaxrsExtension
@Produces({"text/turtle", "application/x-turtle"})
public class StatementsResultTurtleMessageBodyWriter implements MessageBodyWriter<StatementsResultModel> {
	private final Logger logger = LoggerFactory.getLogger(StatementsResultTurtleMessageBodyWriter.class);
	
	@Context
	Request request;
	
	//@Context
    //HttpHeaders headers;
	
	RDFWriterRegistry serviceRegistry = RDFWriterRegistry.getInstance();
	
	//public StatementsResultTurtleMessageBodyWriter() {
	//    System.out.println("StatementsResultTurtleMessageBodyWriter");
	//}
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return StatementsResultModel.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(StatementsResultModel t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(StatementsResultModel model, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,
			Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
	    
	    /*String mimeType = mediaType.toString();
        boolean hasAcceptParam = mimeType != null;

        if (mimeType == null) {
            // Find an acceptable MIME type based on the request headers
            //logAcceptableFormats(request);

            Collection<String> mimeTypes = new LinkedHashSet<>(16);
            // Prefer the default mime types, explicitly before non-default
            for (FileFormat format : serviceRegistry.getKeys()) {
                mimeTypes.add(format.getDefaultMIMEType());
            }
            for (FileFormat format : serviceRegistry.getKeys()) {
                mimeTypes.addAll(format.getMIMETypes());
            }

            mimeType = selectPreferredMIMEType(mimeTypes.iterator(), headers);

            httpHeaders.add("Vary", "Accept");
        }

        if (mimeType != null) {
            Optional<FF> format = serviceRegistry.getFileFormatForMIMEType(mimeType);

            if (format.isPresent()) {
                return serviceRegistry.get(format.get()).get();
            }
        }

        if (hasAcceptParam) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_FILE_FORMAT, mimeType);
            throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
        } else {
            // No acceptable format was found, send 406 as required by RFC 2616
            throw new WebApplicationException("No acceptable file format found.", NOT_ACCEPTABLE);
        }*/
        
		
	    RepositoryConnection conn = model.getConn();
	    Transaction transaction = model.getTransaction();
		try {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, entityStream);
			if(conn != null)
			    conn.exportStatements(model.getSubj(), model.getPred(), model.getObj(), model.isUseInferencing(), rdfWriter, model.getContexts());
			if(transaction != null)
			    transaction.exportStatements(model.getSubj(), model.getPred(), model.getObj(), model.isUseInferencing(), rdfWriter, model.getContexts());
		} catch (InterruptedException e) {
			logger.error("Query interrupted", e);
			throw new WebApplicationException("Query evaluation took too long", Response.Status.SERVICE_UNAVAILABLE);
		} catch (ExecutionException e) {
			logger.error("Serialization error", e);
			throw new WebApplicationException("Serialization error: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }  finally {
			if (conn != null) {
				conn.close();
			}
		}
	}
	
	/**
     * Selects from a set of MIME types, the MIME type that has the highest quality score when matched with the Accept
     * headers in the supplied request.
     * 
     * @param mimeTypes The set of available MIME types.
     * @param request2   The request to match the MIME types against.
     * @return The MIME type that best matches the types that the client finds acceptable, or <tt>null</tt> in case no
     *         acceptable MIME type could be found.
     */
    /*public static String selectPreferredMIMEType(Iterator<String> mimeTypes, HttpHeaders headers) {
        List<MediaType> acceptElements = headers.getAcceptableMediaTypes(); 

        if (acceptElements.isEmpty()) {
            // Client does not specify any requirements, return first MIME type
            // from the list
            if (mimeTypes.hasNext()) {
                return mimeTypes.next();
            } else {
                return null;
            }
        }

        String result = null;
        HeaderElement matchingAcceptType = null;

        double highestQuality = 0.0;

        while (mimeTypes.hasNext()) {
            String mimeType = mimeTypes.next();
            HeaderElement acceptType = matchAcceptHeader(mimeType, acceptElements);

            if (acceptType != null) {
                // quality defaults to 1.0
                double quality = 1.0;

                String qualityStr = acceptType.getParameterValue("q");
                if (qualityStr != null) {
                    try {
                        quality = Double.parseDouble(qualityStr);
                    } catch (NumberFormatException e) {
                        // Illegal quality value, assume it has a different meaning
                        // and ignore it
                    }
                }

                if (quality > highestQuality) {
                    result = mimeType;
                    matchingAcceptType = acceptType;
                    highestQuality = quality;
                } else if (quality == highestQuality) {
                    // found a match with equal quality preference. check if the
                    // accept type is more specific
                    // than the previous match.
                    if (isMoreSpecificType(acceptType, matchingAcceptType)) {
                        result = mimeType;
                        matchingAcceptType = acceptType;
                    }
                }
            }
        }

        return result;
    }*/
    
    /**
     * Tries to match the specified MIME type spec against the list of Accept header elements, returning the applicable
     * header element if available.
     * 
     * @param mimeTypeSpec   The MIME type to determine the quality for, e.g. "text/plain" or "application/xml;
     *                       charset=utf-8".
     * @param acceptElements A List of {@link HeaderElement} objects.
     * @return The Accept header element that matches the MIME type spec most closely, or <tt>null</tt> if no such
     *         header element could be found.
     */
    /*public static MediaType matchAcceptHeader(String mimeTypeSpec, List<MediaType> acceptElements) {
        HeaderElement mimeTypeElem = HeaderElement.parse(mimeTypeSpec);

        while (mimeTypeElem != null) {
            for (MediaType acceptElem : acceptElements) {
                if (matchesAcceptHeader(mimeTypeElem, acceptElem)) {
                    return acceptElem;
                }
            }

            // No match found, generalize the MIME type spec and try again
            mimeTypeElem = generalizeMIMEType(mimeTypeElem);
        }

        return null;
    }*/

}
