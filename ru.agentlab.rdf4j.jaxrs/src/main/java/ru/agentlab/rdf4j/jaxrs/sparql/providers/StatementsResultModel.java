package ru.agentlab.rdf4j.jaxrs.sparql.providers;


import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import ru.agentlab.rdf4j.jaxrs.repository.transaction.Transaction;

public class StatementsResultModel {
    protected RepositoryConnection conn;
    protected Transaction transaction;
    
    protected Resource subj;
    protected IRI pred;
    protected Value obj;
    protected Resource[] contexts;
    protected boolean useInferencing;
    
    public RepositoryConnection getConn() {
        return conn;
    }
    public void setConn(RepositoryConnection conn) {
        this.conn = conn;
    }
    public Transaction getTransaction() {
        return transaction;
    }
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
    
    public Resource getSubj() {
        return subj;
    }
    public void setSubj(Resource subj) {
        this.subj = subj;
    }
    public IRI getPred() {
        return pred;
    }
    public void setPred(IRI pred) {
        this.pred = pred;
    }
    public Value getObj() {
        return obj;
    }
    public void setObj(Value obj) {
        this.obj = obj;
    }
    public Resource[] getContexts() {
        return contexts;
    }
    public void setContexts(Resource[] contexts) {
        this.contexts = contexts;
    }
    public boolean isUseInferencing() {
        return useInferencing;
    }
    public void setUseInferencing(boolean useInferencing) {
        this.useInferencing = useInferencing;
    }
   
}
