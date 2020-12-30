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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
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
public class RepositoryControllerTest extends Rdf4jJaxrsTestSupport {
    
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
    public void createQuryAndDeleteNativeRepository_withRepositoryManagerComponent_ShouldWork() throws Exception {
        // testing a command execution
        //String bundles = executeCommand("bundle:list -t 0");
        //System.out.println(bundles);
        //assertContains("junit", bundles);

        //String features = executeCommand("feature:list -i");
        //System.out.print(features);
        //assertContains("scr", features);

        // using a service and assert state or result
        //RepositoryManagerComponent manager = getOsgiService(RepositoryManagerComponent.class);
        assertNotNull(manager);

        String repId = "id1233";
        assertNull(manager.getRepositoryInfo(repId));
        Repository repository = manager.getOrCreateRepository(repId, "native", null);
        assertNotNull(repository);
        assertNotNull(manager.getRepositoryInfo(repId));
        RepositoryConnection conn = repository.getConnection();

        ModelBuilder builder = new ModelBuilder();
		Model model = builder
			.setNamespace("ex", "http://example.org/")
			.namedGraph("http://www.google.com")
			.subject("ex:Picasso")
				.add(RDF.TYPE, "ex:Artist")		// Picasso is an Artist
				.add(FOAF.FIRST_NAME, "Pablo") 	// his first name is "Pablo"
			.build();
		conn.add(model);

		// We do a simple SPARQL SELECT-query that retrieves all resources of type `ex:Artist`,
		// and their first names.
		String queryString = String.join("\n",
		    "PREFIX ex: <http://example.org/>",
		    "PREFIX foaf: <" + FOAF.NAMESPACE + ">",
		    "SELECT ?s ?n",
		    "WHERE {",
		    "  ?s a ex:Artist ;",
		    "    foaf:firstName ?n .",
		    "}");
		TupleQuery query = conn.prepareTupleQuery(queryString);
		// A QueryResult is also an AutoCloseable resource, so make sure it gets closed when done.
		try (TupleQueryResult result = query.evaluate()) {
			// we just iterate over all solutions in the result...
			BindingSet bs = result.next();
			assertEquals("http://example.org/Picasso", bs.getValue("s").stringValue());
			assertEquals("Pablo", bs.getValue("n").stringValue());
			assertFalse(result.hasNext());

			//while (((TupleQueryResult)queryResult).hasNext()) {
			//	BindingSet solution = ((TupleQueryResult)queryResult).next();
			//	solution.forEach(b -> {
			//		System.out.println(b.getName() + "=" + b.getValue().stringValue());
			//	});
			//}
		}
		conn.close();
        manager.removeRepository(repId);
        assertNull(manager.getRepositoryInfo(repId));
    }

	@Test
	public void createAndDeleteNativeRepository_withRestApi_ShouldWork() throws IOException {
		String repId = "id1234";
		String address = ENDPOINT_ADDRESS + repId;

		ConfigTemplate ct = RepositoryManagerComponent.getConfigTemplate("native");
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("Repository ID", repId);
		String strConfTemplate = ct.render(queryParams);

		//prereq check
		assertNull(manager.getRepositoryInfo(repId));

		WebClient client = WebClient.create(address);
		client.type("text/turtle");
		client.accept(MediaType.WILDCARD);
		Response response = client.put(strConfTemplate);
		assertEquals(204, response.getStatus());
		assertNotNull(manager.getRepositoryInfo(repId));
		client.close();

		WebClient client2 = WebClient.create(address);
		client2.accept(MediaType.WILDCARD);
		Response response2 = client2.delete();
		assertEquals(204, response2.getStatus());
		assertNull(manager.getRepositoryInfo(repId));
		client2.close();
	}

	@Test
	public void reCreateAndDeleteNativeRepository_withRestApi_ShouldWork2ndTime() throws IOException {
		createAndDeleteNativeRepository_withRestApi_ShouldWork();
	}

	@Test
	public void queryShouldWorkOk() throws IOException {
		String repId = "id1235";
        assertNull(manager.getRepositoryInfo(repId));
        Repository repository = manager.getOrCreateRepository(repId, "native", null);
        assertNotNull(repository);
        assertNotNull(manager.getRepositoryInfo(repId));

        RepositoryConnection conn = repository.getConnection();
        ModelBuilder builder = new ModelBuilder();
		Model model = builder
			.setNamespace("ex", "http://example.org/")
			.namedGraph("http://www.google.com")
			.subject("ex:Picasso")
				.add(RDF.TYPE, "ex:Artist")		// Picasso is an Artist
				.add(FOAF.FIRST_NAME, "Pablo") 	// his first name is "Pablo"
			.build();
		conn.add(model);
		conn.close();

		String address = ENDPOINT_ADDRESS + repId;
		WebClient client = WebClient.create(address);
		client.type("application/sparql-query");
		client.accept("application/sparql-results+json");
		Response response = client.post("select ?s ?p ?o where {?s ?p ?o}");
		assertEquals(200, response.getStatus());
		client.close();
	}
}
