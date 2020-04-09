package ru.agentlab.rdf4j.jaxrs.repository.transaction;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.ProtocolUtil;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = TransactionStartController.class, property = {"osgi.jaxrs.resource=true"})
//@Path("/rdf4j-server")
public class TransactionStartController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionStartController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;
   
    @POST
    @Path("/repositories/{repId}/transactions")
    public Response handleRequestInternal(@Context HttpServletRequest request,
                                      @PathParam("repId") String repId) throws Exception {
        logger.info("POST transaction start");
        Repository repository = repositoryManager.getRepository(repId);
        if(repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);
        System.out.print(repository);
        UUID txnId = startTransaction(repository, request);
        if(txnId == null)
            throw new WebApplicationException("Transaction start error for repository with id=" + repId, INTERNAL_SERVER_ERROR);
        logger.info("transaction started");
        
        String txnIdStr = "/repositories/" + repId + "/transactions/" + txnId.toString();
        return Response.created(URI.create(txnIdStr)).build();
    }
    
    private UUID startTransaction(Repository repository, HttpServletRequest request)  throws WebApplicationException {  
        ProtocolUtil.logRequestParameters(request);
        //Map<String, Object> model = new HashMap<String, Object>();
        IsolationLevel isolationLevel = null;
        final String isolationLevelString = request.getParameter(Protocol.ISOLATION_LEVEL_PARAM_NAME);
        if (isolationLevelString != null) {
            final IRI level = SimpleValueFactory.getInstance().createIRI(isolationLevelString);
            for (IsolationLevel standardLevel : IsolationLevels.values()) {
                if (standardLevel.getURI().equals(level)) {
                    isolationLevel = standardLevel;
                    break;
                }
            }
        }
        
        Transaction txn = null;
        UUID txnId = null;
        boolean allGood = false;
        try {
            txn = new Transaction(repository);
            System.out.print(txn);
            txn.begin(isolationLevel);

            txnId = txn.getID();
            System.out.print(txnId);
            
            ActiveTransactionRegistry.INSTANCE.register(txn);
            System.out.print(txn);
            allGood = true;
        } catch (RepositoryException | InterruptedException | ExecutionException e) {
            throw new WebApplicationException("Transaction start error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
        } finally {
            if (!allGood) {
                try {
                    txn.close();
                    System.out.print(txn);
                } catch (InterruptedException | ExecutionException e) {
                    throw new WebApplicationException("Transaction start error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
                }
            }
        }
        return txnId;
    }
}
