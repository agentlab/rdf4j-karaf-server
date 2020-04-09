# rdf4j-karaf-server

JAX-RS OSGi HTTP Whiteboard version of RDF4J server (with Sparql Endpoint and other REST API).

## Building from sources

Build with all tests `mvn clean install`

Quick build without `mvn clean install -P quick`

## Deployment in Karaf

### Run Karaf

`./bin/karaf`

Run from root Karaf floder, not from ./bin folder! See details in https://karaf.apache.org/get-started.html

### Deploy rdf4j REST server to Karaf

Before installation you should build server from sources! (Its due to Karaf installs all from local maven repository by default.)

#### Add feature repository

* `feature:repo-add mvn:ru.agentlab.rdf4j/ru.agentlab.rdf4j.features/3.1.2-SNAPSHOT/xml`

#### Install karaf features and activate OSGi bundles

Install main feature (installs all sub-features):

* `feature:install ru.agentlab.rdf4j.jaxrs`

Or you colud install sub-features one by one:

* `feature:install org.eclipse.rdf4j`
* `feature:install karaf-scr`
* `feature:install karaf-rest-all`
* `feature:install ru.agentlab.rdf4j.jaxrs.deps`
* `feature:install ru.agentlab.rdf4j.jaxrs`

## Development

* `bundle:watch *` -- Karaf should monitor local maven repository and redeploy rebuilded bundles automatically

* `bundle:list` и `la` -- list all plugins
* `feature:list` -- list all features

* `display` -- show logs
* `log:set DEBUG` -- set logger filter into detailed mode

### How to run single testclass?

When you run single testclass from single module Maven did not recompile whole project and bundle under the test! If you need to recompile bundle under the test, do it manually beforehand:

`mvn clean install -P quick`

* Run single OSGi test class (integration tests)
  * without debugger
    * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest`
  * with debugger (on port 5005)
    * edit Rdf4jJaxrsTestSupport.java, uncomment line with code "KarafDistributionOption.debugConfiguration("5005", true)"
    * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest`
* Run single unit test class (i.e. non OSGi) or debug Pax-Exam internals for particular OSGi test class
  * without debugger
    * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest`
  * with debugger (on port 5005)
    * should not edit Rdf4jJaxrsTestSupport.java
    * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest "-Dmaven.surefire.debug"`
