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

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExamParameterized;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ru.agentlab.rdf4j.jaxrs.tests.helpers.Rdf4jJaxrsTestSupportBase;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;



/**
 * Тесты для Statements API с Context-Type = turtle
 *
 */
@RunWith(PaxExamParameterized.class)
@ExamReactorStrategy(PerClass.class)
public class StatementsControllerTest extends Rdf4jJaxrsTestSupportBase {
    @Inject
    protected RepositoryManagerComponent manager;

    String wrongRepAddr;
    String WRONG_TRIPLE_ADDRESS = "&subj=%3Curn:x-local:graph1%3E&pred=<http://purl.org/dc/elements/1.1/publisher>&obj=\"BobUS\"";
    String TRIPLE_ADDRESS;
    String TWO_TRIPLE_ADDRESS;
    String address;

    static String file = "/testcases/default-graph-1.ttl";
    static RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);

    String repId;
    Repository repository;
    RepositoryConnection repositoryCon;
    Model modelBeforeDelete;

    private class Checker{
        String requestAnswer;
        boolean testCheck;
        long longSize;
    }

    private String testType;
    private String grafContext;

    @Configuration
    public static Option[] config() {
        Option[] options = new Option[]{
            // uncomment if you need to debug (blocks test execution and waits for the debugger)
            //KarafDistributionOption.debugConfiguration("5005", true),
        };
        return Stream.of(configBase(), options).flatMap(Stream::of).toArray(Option[]::new);
    }

    @ProbeBuilder
    public static TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        return probeConfigurationBase(probe);
    }

    public StatementsControllerTest(String typeTest,String grafContext){
        this.testType = typeTest;
        this.grafContext = grafContext;
    }

    @Parameters
    public static List<String[]> data(){
        return Arrays.asList(new String[][] {
            {"memory","null"}, {"native","null"}, {"native-rdfs","null"},
            {"memory","%3Cfile%3A%2F%2FC%3A%2Ffakepath%2Fdefault-graph-1.ttl%3E"},
            {"native","%3Cfile%3A%2F%2FC%3A%2Ffakepath%2Fdefault-graph-1.ttl%3E"},
            {"native-rdfs","%3Cfile%3A%2F%2FC%3A%2Ffakepath%2Fdefault-graph-1.ttl%3E"}
        });
    }

    @Before
    public void init() throws Exception {
        super.init();
        
        UUID uuid = UUID.randomUUID();
        repId = uuid.toString();
        System.out.println("repId=" + repId + ", testType=" + testType);
        TWO_TRIPLE_ADDRESS = "&obj=\"Bob\"&pred=<http://purl.org/dc/elements/1.1/publisher>";
        TRIPLE_ADDRESS = "&subj=%3Curn:x-local:graph1%3E&pred=<http://purl.org/dc/elements/1.1/publisher>&obj=\"Bob\""   ;
        wrongRepAddr = ENDPOINT_ADDRESS + "id1237" + "/statements";
        address = ENDPOINT_ADDRESS + repId + "/statements?context=" + grafContext ;
        
        repository = manager.getOrCreateRepository(repId, testType, null);
        repositoryCon = repository.getConnection();
    }

    @After
    public void cleanup() {
        repositoryCon.close();
    }

    public static WebClient webClientCreator(String myAddress){
        WebClient client = WebClient.create(myAddress);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        return client;
    }

    public Model getAllStatemnts(){
        WebClient client2 = webClientCreator(address);
        System.out.println("getAllStat: " + address);
        client2.accept(new MediaType("text", "turtle"));
        Response response2 = client2.get();
        String gotString = response2.readEntity(String.class);
        assertEquals(200, response2.getStatus());
        Reader reader = new StringReader(gotString);
        Model modelFromServer = null;
        try {
            modelFromServer = Rio.parse(reader,"", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        client2.close();
        return modelFromServer;
    }

    public Checker postStatement(String address){
        Checker checker = new Checker();
        WebClient client = webClientCreator(address);
        System.out.println("PostStatements: " + address);
        InputStream dataStream = RepositoryControllerTest.class.getResourceAsStream(file);
        assertNotNull(dataStream);
        try {
            assertThat("dataStream.available", dataStream.available(), greaterThan(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Response response = client.post(dataStream);
        checker.requestAnswer = ""+response.getStatus();
        client.close();
        checker.longSize =repositoryCon.size();
        return checker;
    }

    public boolean isStatementSubset(){
        InputStream dataStream2 = RepositoryControllerTest.class.getResourceAsStream(file);
        Model modelFromFile = null;
        try {
            modelFromFile = Rio.parse(dataStream2,"", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Model modelFromServer = getAllStatemnts();
        return isSubset(modelFromFile,modelFromServer);
    }

    public Checker deletAllStatements(String address){
        Checker checker = new Checker();
        WebClient clientDeleter = webClientCreator(address);
        System.out.println("deleteAdress: "+ address);
        Response responseForDelete = clientDeleter.delete();
        checker.requestAnswer = ""+responseForDelete.getStatus();
        clientDeleter.close();
        Model modelAfterDelete = getAllStatemnts();
        System.out.println("after: " + modelAfterDelete);
        System.out.println("before:  " + modelBeforeDelete);
        checker.testCheck = modelAfterDelete.equals(modelBeforeDelete);
        return checker;
    }

    public Checker deleteOneStatement(String deleteAddrAdd){
        Checker checker = new Checker();
        String triple = "# Default graph\n" +
                "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "\n" +
                "<urn:x-local:graph1> dc:publisher \"Bob\" .";
        String deleteAddress =  address + deleteAddrAdd;
        System.out.println("deleteAdress: " + deleteAddress);
        WebClient client = webClientCreator(deleteAddress);
        Response response = client.delete();
        checker.requestAnswer = "" + response.getStatus();
        client.close();

        Model modelAfterDelete = getAllStatemnts();
        System.out.println(getAllStatemnts());
        Reader reader = new StringReader(triple);
        Model modelTriple = null;
        try {
            modelTriple = Rio.parse(reader, "", RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        checker.testCheck = isSubset(modelTriple, modelAfterDelete);
        return  checker;
    }

    public Checker putTwoStatemnts(String address){
        Checker checker =new Checker();
        String putAddress = address;
        String putTriples = "# Default graph" +
        "@prefix dc: <http://purl.org/dc/elements/1.1/> ." +
        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." +
                "<urn:x-local:graph1> dc:publisher \"Bob\" ." +
                "<urn:x-local:graph2> dc:publisher \"Bob\" .";
        WebClient client = webClientCreator(putAddress);
        Response response = client.put(putTriples);
        checker.requestAnswer = "" + response.getStatus();

        Model gotModel = getAllStatemnts();
        Reader reader = new StringReader(putTriples);
        Model twoTriples = null;
        try {
            twoTriples = Rio.parse(reader,"",RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        checker.testCheck = isSubset(twoTriples,gotModel);
        return checker;
    }

    /**
     * Все данные из POST запроса попадают в репозиторий
     */
    @Test
    public void postStatementsShouldWorkOk() {
        Checker checker;
        checker = postStatement(address);
        assertThat("repositoryCon.size", checker.longSize, equalTo(4L));
        assertThat("postStatementStatus: ", checker.requestAnswer, equalTo("204"));

        assertTrue(isStatementSubset());
        checker = postStatement(wrongRepAddr);
        assertThat("repositoryCon.size", checker.longSize, equalTo(4L)); // выводит тру даже если отправил неправильный адресс
        assertThat("postStatementStatus: ", checker.requestAnswer, equalTo("404"));
    }
    
    @Test
    public void deleteAllStatementsShouldWorkOk() throws IOException {
        modelBeforeDelete = getAllStatemnts();
        Checker checker;
        postStatement(address);
        checker = deletAllStatements(address);
        assertThat("deleteAllStatements(): ", checker.requestAnswer, equalTo("204"));
        assertThat("deleteAllStatements(): ",checker.testCheck,equalTo(true));

        postStatement(wrongRepAddr);
        checker = deletAllStatements(wrongRepAddr);
        assertThat("deleteAllStatements(): ", checker.requestAnswer, equalTo("404"));
    }
    
    @Test
    public void deleteOneStatementShouldWorkOk() throws IOException {
        Checker checker;
        modelBeforeDelete = getAllStatemnts();
        System.out.println("adrrpost" + address);
        postStatement(address);
        checker =deleteOneStatement(TRIPLE_ADDRESS);
        assertThat("deleteOneStatements() Status: ", checker.requestAnswer, equalTo("204"));
        assertThat("deleteOneStatements(): ", checker.testCheck ,equalTo(false));
        
        deletAllStatements(address);
        postStatement(address);
        checker = deleteOneStatement(WRONG_TRIPLE_ADDRESS);
        assertThat("deleteOneStatements() wrong address Status: ", checker.requestAnswer, equalTo("204"));
        assertThat("deleteOneStatements() wrong address : ", isStatementSubset(),equalTo(true));
    }

    @Test
    public void putStatementsShouldWorkOk() throws IOException{
        Checker checker;
        postStatement(address);
        checker = putTwoStatemnts(address);
        assertThat("Put Exists two triples: ", checker.testCheck, equalTo(true));
        assertThat("Put Exists two triples Status: ",checker.requestAnswer, equalTo("204"));

        checker = putTwoStatemnts(wrongRepAddr);
        assertThat("Put Exists two triples Status: ",checker.requestAnswer, equalTo("404"));
    }
    /**
     * POST, DELETE на несуществующий репозиторий ------- done
     * PUT на несуществующий репозиторий
     * DELETE несуществующих триплов в графе (дефолтовом или именованном) из параметра context------ done
     * DELETE существующих триплов (одного или всех) в графе (дефолтовом или именованном) из параметра context -----done
     * POST пары триплов перезаписывает существующие 2 трипла в графе (дефолтовом или именованном) из параметра context
     * PUT пары триплов очищает граф (дефолтовом или именованном) из параметра context ---- done
     */
}
