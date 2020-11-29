package ru.agentlab.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.IRI;

public class FileUploadConfig {

    public String file;
    public String baseURI;
    public IRI graph;
    
    public FileUploadConfig(String file, String baseURI, IRI graph) {
        this.file = file;
        this.baseURI = baseURI;
        this.graph = graph;
    }
}
