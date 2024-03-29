# rdf4j-karaf-server

JAX-RS OSGi HTTP Whiteboard version of RDF4J server (with Sparql Endpoint and other REST API).

## Building from sources

Requirements: Java 11 JDK, Maven 3.6

Build with all tests `mvn clean install`

Quick build without `mvn clean install -P quick`

## Deployment into clean Karaf instance

Requirements:
* Java 16 JDK or JRE
* Karaf 4.3.3

Download and unpack Karaf into folder.

### Configure

Copy all config files from `<this-repository>/distrib/src/main/distribution/etc` into `<karaf-root>/etc`

### Run Karaf

Go into `<karaf-root>` folder

Run from Karaf root floder `./bin/karaf`. Run from Karaf root floder, not from ./bin folder! See details in https://karaf.apache.org/get-started.html

### Deploy rdf4j REST server into Karaf

#### Add feature repository

* `feature:repo-add mvn:ru.agentlab.rdf4j/ru.agentlab.rdf4j.features/4.1.0-SNAPSHOT/xml/features`

#### Install karaf features and activate OSGi bundles

Install main feature (installs all sub-features):

* `feature:install rdf4j-spring`

Or you colud install sub-features one by one:

* `feature:install org.eclipse.rdf4j`
* `feature:install karaf-scr`
* `feature:install ru.agentlab.rdf4j`
* `feature:install karaf-rest-all`
* `feature:install rdf4j-spring`

In case you need log4j2 JSON logger, install feature `feature:install pax-logging-log4j2-extra`

#### How to check if all is working

* If Karaf did not start properly, check that JAVA_HOME points to JDK 11 folder. JDK 13-14 is not supported yet https://issues.apache.org/jira/browse/KARAF-6624

* Check if Jetty is running
  * In a Web Browser
    * go to the http://localhost:8181
    * check if it returns page with "HTTP ERROR 403 Forbidden" header and "powered by Jetty" footer
* Check if Aries Jax-RS Whiteboard is running
  * In a Web Browser
    * go to the http://localhost:8181/system/console/
    * check if it returns page with Karaf Admin Console or login request
* Check REST API to retrieve repositories list
  * In a Web Browser 
    * go to the http://localhost:8181/rdf4j-server/repositories
    * check if it downloads repositories.srx file
  * In HTTP Client (Posmman, etc)
    * send HTTP GET request
      * address http://localhost:8181/rdf4j-server/repositories
      * HTTP header `Accept: application/sparql-results+json`
    * check if it returns empty bindings `"bindings": []` or your actual repositories array
    * check if it returns data in the format corresponded to the 'Accept' header in HTTP request

For further REST API interface see https://rdf4j.org/documentation/reference/rest-api/

## Development

* `bundle:watch *` -- Karaf should monitor local maven repository and redeploy rebuilded bundles automatically

* `bundle:list` и `la` -- list all plugins
* `feature:list` -- list all features

* `display` -- show logs
* `log:set DEBUG` -- set logger filter into detailed mode

### How to run single testclass?

When you run single testclass from single module Maven did not recompile whole project and bundle under the test! If you need to recompile bundle under the test, do it manually beforehand:

`mvn clean install -P quick`

#### Run single OSGi test class (integration tests)

* without debugger
  * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest`
* with debugger (on port 5005)
  * edit Rdf4jJaxrsTestSupport.java, uncomment line with code "KarafDistributionOption.debugConfiguration("5005", true)"
  * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest`

#### Run single unit test class (i.e. non OSGi) or debug Pax-Exam internals for particular OSGi test class

* without debugger
  * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest`
* with debugger (on port 5005)
  * should not edit Rdf4jJaxrsTestSupport.java
  * `mvn clean test -pl ru.agentlab.rdf4j.jaxrs.tests -Dtest=StatementsControllerTest "-Dmaven.surefire.debug"`
