/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.jaxrs.tests.helpers;

import java.util.stream.Stream;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.osgi.framework.Constants;

/**
 * For parameterized tests with RDF4J and Jax-RS (REST API testing).
 * Helper class with removed annotations @Configuration and @ProbeBuilder
 *
 */
public class Rdf4jJaxrsTestSupportBase extends Rdf4jTestSupportBase {
    public String rdf4jServer;
    public String ENDPOINT_ADDRESS;
    
    public static final String MIN_HTTP_PORT = "9080";
    public static final String MAX_HTTP_PORT = "9999";
    
    public void init() throws Exception {
        rdf4jServer = "http://localhost:" + getHttpPort() + "/rdf4j-server/";
        ENDPOINT_ADDRESS = rdf4jServer + "repositories/";
    }
    
    public static TestProbeBuilder probeConfigurationBase(TestProbeBuilder probe) {
        probe.setHeader(Constants.IMPORT_PACKAGE, "org.eclipse.rdf4j.query.algebra.evaluation.impl,org.apache.cxf.jaxrs.client");
        return probe;
    }
    
    public static Option[] configBase() {
    	String httpPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        Option[] options = new Option[]{
        	KarafDistributionOption.features(CoreOptions.maven().groupId("ru.agentlab.rdf4j").artifactId("ru.agentlab.rdf4j.features").type("xml").version("3.1.2-SNAPSHOT"), "ru.agentlab.rdf4j.jaxrs"),
        	KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
        	KarafDistributionOption.editConfigurationFilePut("etc/org.apache.cxf.osgi.cfg", "org.apache.cxf.servlet.context", "/rdf4j-server"),
        };
        return Stream.of(Rdf4jTestSupportBase.configBase(), options).flatMap(Stream::of).toArray(Option[]::new);
    }
}
