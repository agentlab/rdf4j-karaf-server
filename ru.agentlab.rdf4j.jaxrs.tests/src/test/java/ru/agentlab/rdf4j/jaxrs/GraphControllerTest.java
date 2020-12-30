
/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ru.agentlab.rdf4j.jaxrs.tests.helpers.Rdf4jJaxrsTestSupport;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphControllerTest extends Rdf4jJaxrsTestSupport {
    
    @Configuration
    public Option[] config() {
        Option[] options = new Option[]{
             // uncomment if you need to debug (blocks test execution and waits for the debugger)
             //KarafDistributionOption.debugConfiguration("5005", true),
        };
        return Stream.of(super.config(), options).flatMap(Stream::of).toArray(Option[]::new);
    }
    
    protected String address;
    protected String file = "/testcases/default-graph.ttl";
    protected String file1 = "/testcases/default-graph-1.ttl";
    protected String file2 = "/testcases/default-graph-2.ttl";
    protected RDFFormat dataFormat = Rio.getParserFormatForFileName(file1).orElse(RDFFormat.RDFXML);

    protected String repId = "id1238";
    protected String graphNameFirst = "graph1";
    protected String graphNameSecond = "graph2";
    protected RepositoryConnection repositoryCon;
    protected Repository repository;
    protected MediaType turtleMediaType = new MediaType("text", "turtle");
    protected RDFFormat turtleFormat = RDFFormat.TURTLE;
    
    protected LinkedHashModel emptyModel = new LinkedHashModel();
    
    @Inject
    protected RepositoryManagerComponent manager;

    @Before
    public void init() throws Exception {
        super.init();
        
        String graphSection = "/rdf-graphs/";
        address = ENDPOINT_ADDRESS + repId + graphSection;
        repository = manager.getOrCreateRepository(repId, "native", null);
        repositoryCon = repository.getConnection();
    }

    @After
    public void cleanup() {
        repositoryCon.close();
        repository.shutDown();
        manager.removeRepository(repId);
    }

    @Test
    public void shouldAddGetDeleteGraph() throws Exception {
        String graphDefaultUrl = address + "service?default";
        String graph1Url = address + graphNameFirst;
        String graph2Url = address + graphNameSecond;
        Model modelGraphDefaultFile = Rio.parse(RepositoryControllerTest.class.getResourceAsStream(file), "", dataFormat);
        Model modelGraph1File = Rio.parse(RepositoryControllerTest.class.getResourceAsStream(file1), "", dataFormat);
        Model modelGraph2File = Rio.parse(RepositoryControllerTest.class.getResourceAsStream(file2), "", dataFormat);
        LinkedHashModel combinedModel = new LinkedHashModel();
        combinedModel.addAll(modelGraphDefaultFile);
        combinedModel.addAll(modelGraph1File);
        combinedModel.addAll(modelGraph2File);
        
        //System.out.println("Graph Default from server isEmpty");
        getGraphAndCheckEqualModel(graphDefaultUrl, emptyModel);
        
        checkNoContentResponse(postFileToGraph(graphDefaultUrl, turtleFormat, RepositoryControllerTest.class.getResourceAsStream(file), MediaType.WILDCARD_TYPE));
        Model modelGraphDefaultServer1 = parseGraph(getServerGraph(graphDefaultUrl, turtleFormat, turtleMediaType), turtleFormat);
        assertThat("Graph Default from server equals File", modelGraphDefaultServer1, equalTo(modelGraphDefaultFile));
        
        //System.out.println("POST statements to graph1 on address=" + graph1Url);
        checkNoContentResponse(postFileToGraph(graph1Url, turtleFormat, RepositoryControllerTest.class.getResourceAsStream(file1), MediaType.WILDCARD_TYPE));
        
        getGraphAndCheckEqualModel(graphDefaultUrl, modelGraphDefaultFile);
        
        //System.out.println("POST statements to graph2 on address=" + graph2Url);        
        checkNoContentResponse(postFileToGraph(graph2Url, turtleFormat, RepositoryControllerTest.class.getResourceAsStream(file2), MediaType.WILDCARD_TYPE));
        
        getGraphAndCheckEqualModel(graphDefaultUrl, modelGraphDefaultFile);
        
        //System.out.println("GET statements from graph1 on address=" + graph1Url);
        Model modelGraph1Server = getGraphAndCheckEqualModel(graph1Url, modelGraph1File);

        //System.out.println("GET statements from graph2 on address=" + graph2Url);
        Model modelGraph2Server = getGraphAndCheckEqualModel(graph2Url, modelGraph2File);

        checkModelInequality(modelGraph1Server, modelGraph2Server);
        
        //System.out.println("DELETE \"" + graphNameFirst + "\" from repository \"" + repId + "\" on address=" + address + graphNameFirst);
        checkNoContentResponse(deleteGraph(graph1Url, MediaType.WILDCARD_TYPE));
        assertThat("repositoryCon.size decreased by 4 after delete", repositoryCon.size(), equalTo((long)(combinedModel.size() - modelGraph1File.size())));
        
        //System.out.println("Graph2 is intact after Graph1 deletion");
        getGraphAndCheckEqualModel(graph2Url, modelGraph2File);
        
        getGraphAndCheckEqualModel(graphDefaultUrl, modelGraphDefaultFile);
    }
    
    protected Response postFileToGraph(String address, RDFFormat format, InputStream in, MediaType accept) throws IOException {
        WebClient client = WebClient.create(address);
        client.type(format.getMIMETypes().get(0));
        client.accept(accept);
        assertNotNull(in);
        assertThat("dataStream.available", in.available(), greaterThan(0));
        Response response = client.post(in);
        client.close();
        return response;
    }
    
    protected Response getServerGraph(String address, RDFFormat format, MediaType accept) throws IOException {
        WebClient client = WebClient.create(address);
        client.type(format.getMIMETypes().get(0));
        client.accept(accept);
        Response response = client.get();
        assertThat("response media type is compatible", response.getMediaType().isCompatible(accept), equalTo(true));
        client.close();
        return response;
    }
    
    protected Model parseGraph(Response response, RDFFormat format) throws IOException {
        String body = response.readEntity(String.class);
        //System.out.println("BODY FROM GET:\n" + body);
        Reader reader = new StringReader(body);
        return Rio.parse(reader, "", format);
    }
    
    protected Response deleteGraph(String address, MediaType accept) throws IOException {
        WebClient client = WebClient.create(address);
        client.accept(accept);
        Response response = client.delete();
        client.close();
        return response;
    }
    
    protected void checkNoContentResponse(Response response) {
        assertEquals(204, response.getStatus());
        assertThat("repositoryCon.status", response.getStatusInfo().getReasonPhrase(), equalTo("No Content"));
        assertThat("repositoryCon.Body", response.readEntity(String.class), equalTo(""));
    }
    
    protected Model getGraphAndCheckEqualModel(String graphDefaultUrl, Model model) throws IOException {
        Model modelGraphServer = parseGraph(getServerGraph(graphDefaultUrl, turtleFormat, turtleMediaType), turtleFormat);
        assertThat("Graph from server equals Model", modelGraphServer, equalTo(model));
        return modelGraphServer;
    }
    
    protected void checkModelInequality(Model model1, Model model2) {
        assertThat("Two graphs cannot be mapped ot each other", model1, is(not(equalTo(model2))));
        assertThat("Graph1 is not subset of Graph2", !Models.isSubset(model1, model2));
        assertThat("Graph2 is not subset of Graph1", !Models.isSubset(model2, model1));
    }
}
