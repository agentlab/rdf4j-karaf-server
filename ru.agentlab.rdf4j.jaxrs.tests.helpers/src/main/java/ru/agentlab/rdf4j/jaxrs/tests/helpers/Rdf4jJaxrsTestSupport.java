package ru.agentlab.rdf4j.jaxrs.tests.helpers;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;

/**
 * For parameterized tests with RDF4J and Jax-RS (REST API testing).
 * Helper class with annotations @Configuration and @ProbeBuilder
 *
 */
public class Rdf4jJaxrsTestSupport extends Rdf4jJaxrsTestSupportBase {
    @Configuration
    public Option[] config() {
        return configBase();
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        System.out.println("TestProbeBuilder gets called");
        //probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        probeConfigurationBase(probe);
        return probe;
    }
}
