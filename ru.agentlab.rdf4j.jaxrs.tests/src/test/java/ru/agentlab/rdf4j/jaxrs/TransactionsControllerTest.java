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

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.junit.runners.Parameterized;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExamParameterized;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ru.agentlab.rdf4j.jaxrs.tests.helpers.Rdf4jJaxrsTestSupportBase;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@RunWith(PaxExamParameterized.class)
@ExamReactorStrategy(PerClass.class)
public class TransactionsControllerTest extends Rdf4jJaxrsTestSupportBase {

    @Inject
    protected RepositoryManagerComponent manager;

    private String address;
    private String addressGetStatements;
    final private String file = "/testcases/default-graph-1.ttl";
    final private String fileDelete = "/testcases/default-graph-1deleted.ttl";
    final private String fileHalf = "/testcases/default-graph-1half.ttl";
    final private String file2 = "/testcases/default-graph.ttl";
    RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);
    
    String repId;
    final String ACTION = "action=";
    String COMMIT = "COMMIT";
    final String ADD = "ADD";
    final String GET = "GET";
    final String SIZE = "SIZE";
    final String DELETE = "DELETE";
    Repository repository;
    RepositoryConnection repositoryCon;
    private final String testType;

    @Configuration
    public static Option[] config2() {
        Option[] options = new Option[]{
            // uncomment if you need to debug (blocks test execution and waits for the debugger)
            //KarafDistributionOption.debugConfiguration("5005", true),
        };
        return Stream.of(configBase(), options).flatMap(Stream::of).toArray(Option[]::new);
    }

    @ProbeBuilder
    public static TestProbeBuilder probeConfiguration2(TestProbeBuilder probe) {
        return probeConfigurationBase(probe);
    }

    public TransactionsControllerTest(String typeTest){
        this.testType = typeTest;
    }
    @Parameterized.Parameters
    public static List<String[]> data(){
        return Arrays.asList(new String[][]{
            {"memory"},{"native"}//,{"native-rdfs"}
        });
    }

    @Before
    public void init() throws Exception {
        super.init();
        
        UUID uuid = UUID.randomUUID();
        repId = uuid.toString();
        //repId = "rashid";
        //ENDPOINT_ADDRESS = "https://agentlab.ru/" +"rdf4j-server"+ "/repositories/";
        address = ENDPOINT_ADDRESS + repId + "/transactions";
        addressGetStatements = ENDPOINT_ADDRESS + repId + "/statements";
        
        System.out.println("create repository, type=" + testType + ", repId="  + repId);
        repository = manager.getOrCreateRepository(repId, testType, null);
        repositoryCon = repository.getConnection();
    }
    
    @After
    public void cleanup() {
        cleanreapository();
        repositoryCon.close();
        repository.shutDown();
        manager.removeRepository(repId);
    }

    public void cleanreapository(){
        WebClient client = webClientCreator(addressGetStatements);
        Response response = client.delete();
        client.close();
    }

    public WebClient webClientCreator(String myAddress){
        WebClient client = WebClient.create(myAddress);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        return client;
    }

    public Model modelCreator(InputStream inputStream, String baseUrl, RDFFormat rdfFormat){
        Reader reader = new InputStreamReader(inputStream);
        Model model = null;
        try {
            model = Rio.parse(reader,baseUrl,rdfFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;
    }
    public Model modelCreator(String string,String baseUrl,RDFFormat rdfFormat){
        Reader reader = new StringReader(string);
        Model model = null;
        try {
            model = Rio.parse(reader, baseUrl,rdfFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;
    }
    public Model getStatementsFromServer(){
        WebClient client = webClientCreator(addressGetStatements);
        client.accept(new MediaType("text", "turtle"));
        Response response = client.get();
        String gotString = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        Model modelFromServer = modelCreator(gotString,"",RDFFormat.TURTLE);
        client.close();
        return modelFromServer;
    }

    synchronized String createTransaction() {
        WebClient client = webClientCreator(address);
        System.out.println("create transaction "  + address);
        Response response = client.post(null);
        assertEquals(201, response.getStatus());
        client.close();
        return response.getHeaderString("Location");
    }

    protected void addToTransaction(String transAddress, String fileAdd){
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(file);
        WebClient client = webClientCreator(transAddress + "?" + ACTION + ADD);
        Response response = client.put(inputStream);
        assertThat("addToTransactionError:", response.getStatus(), equalTo(200));
        client.close();
    }

    protected String getDataFromTransaction(String transAddress){
        WebClient client = webClientCreator(transAddress + "?" + ACTION + GET);
        Response response = client.put(null);
        client.close();
        return  response.readEntity(String.class);
    }

    protected Response commitTransaction(String transAddress){
        WebClient client = webClientCreator(transAddress + "?" + ACTION + COMMIT);
        Response response = client.put(null);
        client.close();
        return response;
    }

    protected void deleteDataInTransAction(String transAddress, String myFile){
        WebClient client = webClientCreator(transAddress + "?" + ACTION + DELETE);
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(myFile);
        Response response  = client.post(inputStream);
        assertThat("delteDataInTransactionError:", response.getStatus(), equalTo(200));
        client.close();
    }

    protected Response deleteTransaction(String transAddress){
        WebClient client = webClientCreator(transAddress);
        Response response = client.delete();
        client.close();
        return response;
    }

    protected int getSizeOfTransaction(String transAddress){
        WebClient client = webClientCreator(transAddress + "?" + ACTION + SIZE);
        Response response = client.put(null);
        client.close();
        return response.readEntity(Integer.class);
    }


    @Test
    public void commitingTransactionShouldWorkOK() throws IOException {
        String transAddress = createTransaction();
        addToTransaction( transAddress, file);
        Response response = commitTransaction(transAddress);
        assertThat("commitTransactionError:in commitingTransactionShouldWorkOK", response.getStatus(), equalTo(200));
        Model modelFormServer = getStatementsFromServer();
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(file);
        Model modelFormFile = modelCreator(inputStream,"",RDFFormat.TURTLE);
        assertThat("commitTransactionsShouldWorkOk:", isSubset(modelFormFile, modelFormServer), equalTo(true));
    }

    @Test
    public void clearBeforeCommitingShouldAddNoChange(){
        Model modelBeforeAction = getStatementsFromServer();
        String transAddress = createTransaction();
        addToTransaction(transAddress,file);
        deleteDataInTransAction(transAddress,file);
        Response response = commitTransaction(transAddress);
        assertThat("commitTransactionError:in clearBeforeCommitingShouldAddNoChange", response.getStatus(), equalTo(200));
        Model modelAfterAction = getStatementsFromServer();
        assertThat("clearBeforeCommitingShouldAddNoChange: ", modelAfterAction.equals(modelBeforeAction), equalTo(true));
    }

    @Test
    public void tryDeleteAfterCommit(){
        String transAddress = createTransaction();
        addToTransaction(transAddress, file);
        Response responseCommit = commitTransaction(transAddress);
        assertThat("commitTransactionError:in tryDeleteAfterCommit", responseCommit.getStatus(), equalTo(200    ));
        Response response = deleteTransaction(transAddress);
        assertThat("tryDeleteAfterCommit ", response.getStatus(), equalTo(500));
    }

    @Test
    public void commitAfterRollbackShouldGetError(){
        Model modelBeforeAction =getStatementsFromServer();
        String transAddress = createTransaction();
        addToTransaction(transAddress,file);
        deleteTransaction(transAddress);
        Response response  = commitTransaction(transAddress);
        assertThat("commitTransactionError:in commitAfterRollbackShouldGetError()", response.getStatus(), equalTo(500));
        Model modelAfterAction = getStatementsFromServer();
        assertThat("commitAfterRollbackShouldGetError(): do not any changes", modelAfterAction.equals(modelBeforeAction), equalTo(true));
    }

    @Test
    public void addTwoTransactionCommitOneShouldWorkOK(){
        String transAddress = createTransaction();
        String transAddressSecond = createTransaction();
        addToTransaction(transAddress, file);
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(file);
        Model modelFromFirstFile = modelCreator(inputStream, "", RDFFormat.TURTLE);

        addToTransaction(transAddressSecond,file2);
        InputStream inputStream2 = TransactionsControllerTest.class.getResourceAsStream(file2);
        Model modelFromSecondFile = modelCreator(inputStream2, "", RDFFormat.TURTLE);

        Response response = deleteTransaction(transAddressSecond);
        assertThat("addTwoTransactionCommitOneShouldWorkOK: delete", response.getStatus(), equalTo(204));
        commitTransaction(transAddress);
        Response responseAfterDelete = commitTransaction(transAddressSecond);
        assertThat("commitTransactionError:in commitAfterRollbackShouldGetError()", responseAfterDelete.getStatus(), equalTo(500));
        Model modelAfterAction = getStatementsFromServer();
        assertThat("addTwoTransactionCommitOneShouldWorkOK: file should be",
                isSubset(modelFromFirstFile,modelAfterAction), equalTo(true));
        assertThat("addTwoTransactionCommitOneShouldWorkOK:file should not be",
                isSubset(modelFromSecondFile,modelAfterAction), equalTo(false));
    }

    @Test
    public void getChangesFromTransactionOK(){
        String transAddress = createTransaction();
        addToTransaction(transAddress,file);
        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(file);
        Model modelFromFile = modelCreator(inputStream, "", RDFFormat.TURTLE);

        Model modelGotFromTrans = modelCreator(getDataFromTransaction(transAddress), "", RDFFormat.TURTLE);
        assertThat("getChangesFromTransactionOK", modelFromFile.equals(modelGotFromTrans), equalTo(true));
        deleteTransaction(transAddress);
    }

    @Test
    public void deleteOneStatementFromTransaction(){
        String transAddress = createTransaction();
        addToTransaction(transAddress,file);
        InputStream inputStreamAll = TransactionsControllerTest.class.getResourceAsStream(file);
        Model model4Statements = modelCreator(inputStreamAll, "", RDFFormat.TURTLE);

        deleteDataInTransAction(transAddress, fileDelete);
        commitTransaction(transAddress);

        InputStream inputStream = TransactionsControllerTest.class.getResourceAsStream(fileDelete);
        Model model2Statemnts = modelCreator(inputStream,"",RDFFormat.TURTLE);
        InputStream inputStreamHalf = TransactionsControllerTest.class.getResourceAsStream(fileHalf);
        Model modelHalf = modelCreator(inputStreamHalf,"",RDFFormat.TURTLE);

        Model fromServer = getStatementsFromServer();
        assertThat("deleteOneStatementFromTransaction: deleted model is not subset", isSubset(model2Statemnts,fromServer), equalTo(false));
        assertThat("deleteOneStatementFromTransaction: complete  model is not subset ", isSubset(model4Statements,fromServer), equalTo(false));
        assertThat("deleteOneStatementFromTransaction: half is subset", isSubset(modelHalf, fromServer), equalTo(true));
    }

    @Test
    public void addTwoInOneTrAndTheSameInOtherOK(){
        String transAddress = createTransaction();
        addToTransaction(transAddress,file);

        addToTransaction(transAddress,fileHalf);
        int size = getSizeOfTransaction(transAddress);
        commitTransaction(transAddress);

        assertThat("addTwoInOneTrAndTheSameInOtherOK: size is 4", size, equalTo(4));
    }
}
