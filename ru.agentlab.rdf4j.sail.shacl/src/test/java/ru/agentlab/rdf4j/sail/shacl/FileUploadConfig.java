/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
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
