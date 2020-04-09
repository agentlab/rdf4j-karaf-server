package ru.agentlab.rdf4j.jaxrs;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;

public class Rdf4jJaxrsTestSupport extends Rdf4jJaxrsTestSupport2 {
    @Configuration
    public Option[] config() {
        return configBase();
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        System.out.println("TestProbeBuilder gets called");
        // probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE,
        // "*,org.apache.felix.service.*;status=provisional");
        //probe.setHeader(Constants.IMPORT_PACKAGE, "org.eclipse.rdf4j.query.algebra.evaluation.impl,org.apache.cxf.jaxrs.client");
        probeConfigurationBase(probe);
        return probe;
    }
}
