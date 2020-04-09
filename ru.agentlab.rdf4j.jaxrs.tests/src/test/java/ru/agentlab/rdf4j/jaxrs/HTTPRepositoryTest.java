package ru.agentlab.rdf4j.jaxrs;
import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.stream.Stream;

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HTTPRepositoryTest extends  Rdf4jJaxrsTestSupport{
    
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

    String file =  "/testcases/default-graph-1.ttl";
    Repository repository;
    Repository repo;
    RepositoryConnection repocon;
    Repository rep;
    RepositoryConnection repcon;
    
    String repositoryID;
    String address;
    Resource [] context = new Resource[] {};
    ValueFactory f;
    final String strShouldBe = "<urn:x-local:graph1>"
            +"<http://purl.org/dc/elements/1.1/publisher>"
            +"\"Bob63\" .";

    RDFFormat dataFormat = Rio.getParserFormatForFileName(file).orElse(RDFFormat.RDFXML);

    @Before
    public void init() throws Exception {
        super.init();
        
        repositoryID = "12345648";
        repository = manager.getOrCreateRepository(repositoryID, "memory", null);
        //rdf4jServer = "https://agentlab.ru" + "/rdf4j-server/";
        address = rdf4jServer + "repositories/" + repositoryID + "/statements";
        rep = new HTTPRepository(rdf4jServer, repositoryID);
        repcon = rep.getConnection();
        f = repcon.getValueFactory();
        repo = new SPARQLRepository(rdf4jServer + "repositories/" + repositoryID);
        repo.init();
        repocon = repo.getConnection();
    }

    @After
    public void cleanup() {
        repcon.close();
        repository.shutDown();
//        manager.removeRepository(repositoryID);
    }

    public WebClient webClientCreator(String myAddress){
        WebClient client = WebClient.create(myAddress);
        client.type(dataFormat.getDefaultMIMEType());
        client.accept(MediaType.WILDCARD);
        return client;
    }

    public Model modelCreator(InputStream inputStream,String baseUrl,RDFFormat rdfFormat){
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

    public Model getStatements(){
        WebClient client2 = webClientCreator(address);
        client2.accept(new MediaType("text", "turtle"));
        Response response2 = client2.get();
        String gotString = response2.readEntity(String.class);
        checkerResponseIsOk(response2);
        Model modelFromServer = modelCreator(gotString,"",RDFFormat.TURTLE);
        client2.close();
        return modelFromServer;
    }

    public void checkerResponseIsOk(Response response){
        assertEquals(200, response.getStatus());
    }

    public void checkerNotNullAndGreaterThan(InputStream inputStream, Integer integer) throws IOException {
        assertNotNull(inputStream);
        assertThat("dataStream.available", inputStream.available(), greaterThan(0));
    }

    public boolean checkerIsModelSubset(Model model1, Model model2){
        return isSubset(model1,model2);
    }
    public boolean checkerIsFistSustetSeconNot(Model model1, Model model2, Model mainModel){
        return (isSubset(model1,mainModel)& !isSubset(model2,mainModel));
    }
    public boolean checkerIsModelsEquals(Model model1, Model model2){
        return model1.equals(model2);
    }
    public Model addHTTPRep() throws IOException {
        InputStream input = HTTPRepositoryTest.class.getResourceAsStream(file);
        checkerNotNullAndGreaterThan(input,0);

        Model model = modelCreator(input,"",RDFFormat.TURTLE);
        repcon.add(HTTPRepositoryTest.class.getResource(file), "", dataFormat, context);

        return model;
    }

    public void deleteHTTPRep(){
        repcon.clear(context);
    }

    public Model sparqlSelect(){
        String selectedStr =  "# Default graph" +
                "@prefix dc: <http://purl.org/dc/elements/1.1/> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";

        String queryString = "SELECT ?x ?p ?y WHERE { ?x ?p ?y } ";
        TupleQuery tupleQuery = repocon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Value valueOfX = bindingSet.getValue("x");
                Value valueOfP = bindingSet.getValue("p");
                Value valueOfY = bindingSet.getValue("y");
                selectedStr= selectedStr + "<" + valueOfX + "> " + "<" + valueOfP + "> "
                        + valueOfY + " ."+ "\n";
            }
        }
        Model modelFromSelect = modelCreator(selectedStr,"",RDFFormat.TURTLE);
        return modelFromSelect;
    }

    public void sparqlUpdate() {
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n";
        queryString += "DELETE {?s ?p \"Bob\"}\n";
        queryString += "INSERT {" + strShouldBe + "}\n";
        queryString += "WHERE {?s ?p ?o .}";

        Update tupleQuery =repcon.prepareUpdate(QueryLanguage.SPARQL,queryString);
        tupleQuery.execute();
    }

    @Test
    public void httpRepositoryShouldWorkOk() throws IOException {
        Model resultRawRepository = getStatements();
        assertThat("AddHTTPRepo is Match: ", addHTTPRepChecker(), equalTo(true));

        assertThat("sparql is Match: ", sparqlSelectChecker(), equalTo(true));

        assertThat("update is Match: ", sparqlUpdateChecker(), equalTo(true));

        assertThat("deleteHTTPRepo is Match: ", deleteHTTPRepChecker(resultRawRepository), equalTo(true));
    }

    protected boolean addHTTPRepChecker() throws IOException {
        Model model = addHTTPRep();
        return checkerIsModelSubset(model,getStatements());
    }

    protected boolean deleteHTTPRepChecker(Model modelBeforeDelete){
        deleteHTTPRep();
        return checkerIsModelsEquals(getStatements(),modelBeforeDelete);
    }

    protected boolean sparqlSelectChecker(){
        InputStream inputStream = HTTPRepositoryTest.class.getResourceAsStream(file);
        return checkerIsModelsEquals(sparqlSelect(),modelCreator(inputStream,"",RDFFormat.TURTLE));
    }

    protected boolean sparqlUpdateChecker(){
        sparqlUpdate();

        String nonBe = "<urn:x-local:graph1>"
                +"<http://purl.org/dc/elements/1.1/publisher>"
                +"\"Bob\" .";
        Model modelInserted = modelCreator(strShouldBe, "", RDFFormat.TURTLE);
        Model modelDeleted = modelCreator(nonBe, "", RDFFormat.TURTLE);
        Model modelAfterUpdate = getStatements();

        return checkerIsFistSustetSeconNot(modelInserted,modelDeleted,modelAfterUpdate);
    }


}
