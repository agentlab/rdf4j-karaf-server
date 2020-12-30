/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ru.agentlab.rdf4j.jaxrs.tests.helpers.Rdf4jJaxrsTestSupport;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NamespacesControllerTest extends Rdf4jJaxrsTestSupport {

    @Configuration
    public Option[] config() {
        Option[] options = new Option[]{
             // uncomment if you need to debug (blocks test execution and waits for the debugger)
             //KarafDistributionOption.debugConfiguration("5005", true),
        };
        return Stream.of(super.config(), options).flatMap(Stream::of).toArray(Option[]::new);
    }
    
    @Inject
    protected RepositoryManagerComponent manager;
    
    @Before
    public void init() throws Exception {
        super.init();
    }
    
    @Test
    public void queryShouldWorkOk() throws IOException {
        String repId = "id1245";
        assertNull(manager.getRepositoryInfo(repId));
        Repository repository = manager.getOrCreateRepository(repId, "native-rdfs", null);
        assertNotNull(repository);
        assertNotNull(manager.getRepositoryInfo(repId));
        
        InputStream dataStream = RepositoryControllerTest.class.getResourceAsStream(StatementsControllerTest.file);
        assertNotNull(dataStream);
        try {
            assertThat("dataStream.available", dataStream.available(), greaterThan(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        RepositoryConnection repositoryCon = repository.getConnection();
        repositoryCon.add(dataStream, "http://sdfdsf.rt", StatementsControllerTest.dataFormat, new Resource[0]);
        
        String address = ENDPOINT_ADDRESS + repId + "/namespaces";
        WebClient client = WebClient.create(address);
        client.accept("application/sparql-results+json");
        Response response = client.get();
        assertEquals(200, response.getStatus());
        String s = response.readEntity(String.class);
        assertTrue(s.contains("http://www.w3.org/2001/XMLSchema#"));
        assertTrue(s.contains("http://purl.org/dc/elements/1.1/"));
    }
}
