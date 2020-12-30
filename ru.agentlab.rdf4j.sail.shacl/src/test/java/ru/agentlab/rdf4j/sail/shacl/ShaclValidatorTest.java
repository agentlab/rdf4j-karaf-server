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
package ru.agentlab.rdf4j.sail.shacl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.config.DirectTypeHierarchyInferencerConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.agentlab.rdf4j.sail.shacl.sparqled.config.SparqledShaclSailConfig;
import ru.agentlab.rdf4j.sail.shacl.sparqled.config.SparqledShaclSailFactory;

public class ShaclValidatorTest {
    
    @ClassRule
	public static TemporaryFolder tempDir = new TemporaryFolder();
	
	private static final String TEST_REPO = "test";
	
	public static final FileUploadConfig[] vocabsFiles = {
	    new FileUploadConfig("../../rdf-data-expert/vocabs/rdf.ttl", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>", null),
		new FileUploadConfig("../../rdf-data-expert/vocabs/rdfs.ttl", "<http://www.w3.org/2000/01/rdf-schema#>", null),
		new FileUploadConfig("../../rdf-data-expert/vocabs/xsd.ttl", "<http://www.w3.org/2001/XMLSchema#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/xsd-ru.ttl", "<http://cpgu.kbpm.ru/ns/xsd#>", null),
	    //new FileUploadConfig("../../rdf-data-expert/vocabs/shacl.ttl", "<http://www.w3.org/ns/shacl#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/navigation.ttl", "<http://cpgu.kbpm.ru/ns/rm/navigation#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/rm.ttl", "<http://cpgu.kbpm.ru/ns/rm/rdf#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/rm-user-types.ttl", "<http://cpgu.kbpm.ru/ns/rm/user-types#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/cpgu.ttl", "<http://cpgu.kbpm.ru/ns/rm/cpgu#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/acl.ttl", "<http://www.w3.org/ns/auth/acl#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/ppo.ttl", "<http://vocab.deri.ie/ppo#>", null),
	    new FileUploadConfig("../../rdf-data-expert/vocabs/ppo-roles.ttl", "<https://agentlab.ru/onto/ppo-roles#>", null)
	};
	public static final FileUploadConfig[] shapesFiles = {
		new FileUploadConfig("../../rdf-data-expert/shapes/shacl/ppo-roles-shapes.ttl", "<https://agentlab.ru/onto/ppo-roles#>", RDF4J.SHACL_SHAPE_GRAPH),
		//new FileUploadConfig("../../rdf-data-expert/shapes/shacl/shacl-shacl.ttl", "<http://www.w3.org/ns/shacl-shacl#>", RDF4J.SHACL_SHAPE_GRAPH),
	    new FileUploadConfig("../../rdf-data-expert/shapes/shacl/xsd-shapes.ttl", "<http://cpgu.kbpm.ru/ns/xsd-shapes#>", RDF4J.SHACL_SHAPE_GRAPH),
	    new FileUploadConfig("../../rdf-data-expert/shapes/shacl/rm/rm-shapes.ttl", "<http://cpgu.kbpm.ru/ns/rm/rdf#>", RDF4J.SHACL_SHAPE_GRAPH),
		new FileUploadConfig("../../rdf-data-expert/shapes/shacl/rm/rm-user-types-shapes.ttl", "<http://cpgu.kbpm.ru/ns/rm/user-types#>", RDF4J.SHACL_SHAPE_GRAPH),
		new FileUploadConfig("../../rdf-data-expert/shapes/shacl/cpgu/cpgu-shapes.ttl", "<http://cpgu.kbpm.ru/ns/rm/cpgu#>", RDF4J.SHACL_SHAPE_GRAPH)
	};
	public static final FileUploadConfig[] usersFiles = {
	    new FileUploadConfig("../../rdf-data-expert/data/users.ttl", "<http://cpgu.kbpm.ru/ns/rm/users#>", null),
	    new FileUploadConfig("../../rdf-data-expert/data/access-management.ttl", "<http://cpgu.kbpm.ru/ns/rm/policies#>", null)
	};
	public static final FileUploadConfig[] projectsFoldersFiles = {
	    new FileUploadConfig("../../rdf-data-expert/data/projects.ttl", "<http://cpgu.kbpm.ru/ns/rm/projects#>", null),
	    new FileUploadConfig("../../rdf-data-expert/data/folders.ttl", "<http://cpgu.kbpm.ru/ns/rm/folders#>", null)
	};
	public static final FileUploadConfig[] samplesFiles = {
	    new FileUploadConfig("../../rdf-data-expert/data/cpgu/sample-module.ttl", "<http://cpgu.kbpm.ru/ns/rm/reqs#>", null),
	    new FileUploadConfig("../../rdf-data-expert/data/cpgu/sample-collection.ttl", "<http://cpgu.kbpm.ru/ns/rm/reqs#>", null)
	};

	
	protected Sail createSail() {
		try {
			NotifyingSail sailStack = new NativeStore(tempDir.newFolder("nativestore"), "spoc,posc");
			sailStack = new SchemaCachingRDFSInferencer(sailStack);
			return sailStack;
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}
	
	protected static Repository createRepo() throws IOException {
		LocalRepositoryManager manager = new LocalRepositoryManager(tempDir.newFolder("reps"));
		
		SparqledShaclSailConfig sparqledShaclSailConfig = new SparqledShaclSailConfig(
                new DirectTypeHierarchyInferencerConfig(
                    new SchemaCachingRDFSInferencerConfig(
                        new NativeStoreConfig("spoc,posc")
                    )
                )
            );
		manager.addRepositoryConfig(
			new RepositoryConfig(TEST_REPO,
				new SailRepositoryConfig(
				        sparqledShaclSailConfig
				)
			)
		);
		return manager.getRepository(TEST_REPO);
	}
	
	public static Model loadAll(FileUploadConfig[] files) throws RDFParseException, UnsupportedRDFormatException, FileNotFoundException, IOException {
		Model data = new LinkedHashModel();
		for (FileUploadConfig f : files) {
			data.addAll(Rio.parse(new FileInputStream(f.file), f.baseURI, RDFFormat.TURTLE));
			System.out.println("Added " + f.file);
		}
		return data;
	}
	
	public void checkClassShapeIsInserted(RepositoryConnection connection, String classShapeUri, String classUri) throws IOException {
        String selectQueryString = "PREFIX cpgu: <http://cpgu.kbpm.ru/ns/rm/cpgu#>\n"
        	+ "PREFIX sh: <http://www.w3.org/ns/shacl#>\n"
        	+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
        	+ "SELECT ?classShape FROM <http://rdf4j.org/schema/rdf4j#SHACLShapeGraph> WHERE {\n"
        	+ "  ?classShape\n"
        	+ "    a sh:NodeShape ;\n"
        	+ "    sh:targetClass <" + classUri + "> .\n"
        	+ "}";
        Iteration<BindingSet, QueryEvaluationException> bindingSets = connection.prepareTupleQuery(QueryLanguage.SPARQL, selectQueryString).evaluate();
        boolean shapeInSelectResults = false;
        while (bindingSets.hasNext()) {
        	BindingSet bindingSet = bindingSets.next();
        	if (bindingSet.getBinding("classShape").getValue().stringValue().equals(classShapeUri)) {
        		shapeInSelectResults = true;
        		break;
        	}
        }
        assertEquals(true, shapeInSelectResults);
    }
	
	public static void printValidationReport(Exception exception) {
	    Throwable cause = exception.getCause();
        if (cause instanceof ShaclSailValidationException) {
            ValidationReport validationReport = ((ShaclSailValidationException) cause).getValidationReport();
            Model validationReportModel = ((ShaclSailValidationException) cause).validationReportAsModel();
            // use validationReport or validationReportModel to understand validation violations

            Rio.write(validationReportModel, System.out, RDFFormat.TURTLE);
        }
	}
	
	static Repository sailRepository;
	
	@BeforeClass
	public static void init() throws Exception {
		SailRegistry.getInstance().add(new SparqledShaclSailFactory());
		try {
			sailRepository = createRepo();
		} catch (IOException ex) {
			ex.printStackTrace();
			throw ex;
		}
		sailRepository.init();
		
		RepositoryConnection connection = sailRepository.getConnection();
		
		try {
			connection.begin();
			connection.add(loadAll(vocabsFiles), vocabsFiles[0].graph);
			connection.commit();
			
			connection.begin();
			connection.add(loadAll(usersFiles), usersFiles[0].graph);
			connection.commit();
			
			connection.begin();
			connection.add(loadAll(projectsFoldersFiles), projectsFoldersFiles[0].graph);
			connection.commit();
			
			connection.begin();
			connection.add(loadAll(shapesFiles), shapesFiles[0].graph);
			connection.commit();
			
			connection.begin();
			connection.add(loadAll(samplesFiles), samplesFiles[0].graph);
			connection.commit();
		} catch (RepositoryException | RDFParseException | UnsupportedRDFormatException | IOException ex) {
		    printValidationReport(ex);
		    throw ex;
		} finally {
			connection.close();
		}
    }

	@Test
	public void testDirectSubClassOf () throws IOException {
		RepositoryConnection connection = sailRepository.getConnection();
		System.out.println("sesame:directSubClassOf");
		String queryString = String.join("\n", 
    		"SELECT ?o WHERE {",
    		"  <http://cpgu.kbpm.ru/ns/rm/cpgu#Portal> sesame:directSubClassOf ?o .",
    		"}"
		);
		TupleQuery query = connection.prepareTupleQuery(queryString);
		query.setIncludeInferred(true);
		TupleQueryResult result = query.evaluate();
		while (result.hasNext()) {
			BindingSet next = result.next();
			System.err.println(next.getBinding("o").getValue());
		}
		connection.close();
	}
	
	@Test
    public void testSelectShapes () throws IOException {
	    RepositoryConnection connection = sailRepository.getConnection();
		System.out.println("Select SHACL Shapes");
		String queryString = String.join("\n", 
    		"PREFIX sh: <http://www.w3.org/ns/shacl#> ",
    		"SELECT ?s FROM <http://rdf4j.org/schema/rdf4j#SHACLShapeGraph> WHERE {",
    		"  ?s a sh:NodeShape .",
    		"}"
		);
		TupleQuery query = connection.prepareTupleQuery(queryString);
		query.setIncludeInferred(true);
		TupleQueryResult result = query.evaluate();
		while (result.hasNext()) {
			BindingSet next = result.next();
			System.err.println(next.getBinding("s").getValue());
		}
		connection.close();
	}
	
	@Test
    public void testInsertWrongData() throws IOException {
	    RepositoryConnection connection = sailRepository.getConnection();
	    System.out.println("Check Wrong Data With Existing Shapes");
        String queryString = String.join("\n",
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>",
            "INSERT DATA {",
            "  xsd:typeWithoutLabel a rdfs:Datatype .",
            "}"
        );
        try {
            connection.begin();
            connection.prepareUpdate(queryString).execute();
            connection.commit();
            fail();
        } catch (RepositoryException ex) {
            assertTrue(ex.getCause() instanceof ShaclSailValidationException);
        }
        
        queryString = String.join("\n", 
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>",
            "SELECT ?p ?o WHERE {",
            "  xsd:typeWithoutLabel ?p ?o .",
            "}"
        );
            
        TupleQuery query = connection.prepareTupleQuery(queryString);
        query.setIncludeInferred(true);
        TupleQueryResult result = query.evaluate();
        while (result.hasNext()) {
            BindingSet next = result.next();
            System.err.println(next.getBinding("p").getValue() + " " + next.getBinding("o").getValue());
        }
        connection.close();
    }
	
	@Test
    public void testInsertShape () throws IOException {
	    RepositoryConnection connection = sailRepository.getConnection();
	    String queryString = String.join("\n",
    	    "PREFIX cpgu: <http://cpgu.kbpm.ru/ns/rm/cpgu#>",
    	    "PREFIX rm: <http://cpgu.kbpm.ru/ns/rm/rdf#>",
    	    "PREFIX sh: <http://www.w3.org/ns/shacl#>",
    	    "INSERT DATA {",
    	    "  GRAPH <http://rdf4j.org/schema/rdf4j#SHACLShapeGraph> {",
    	    "    cpgu:MissingClassShape ",
    	    "      a sh:NodeShape ;",
    	    "      sh:targetClass cpgu:MissingClass ;",
    	    "      sh:property rm:titleShape .", // required property with sh:minCount = 1 and sh:maxCount = 1
    	    "  }",
    	    "}"
	    );
	    
	    connection.begin();
		connection.prepareUpdate(queryString).execute();
		connection.commit();
		
		System.out.println("Select SHACL Shapes2");
		queryString = String.join("\n", 
    		"PREFIX sh: <http://www.w3.org/ns/shacl#>",
    		"SELECT ?s FROM <http://rdf4j.org/schema/rdf4j#SHACLShapeGraph> WHERE {",
    		"  ?s a sh:NodeShape .",
    		"}"
		);
		/*TupleQuery query = connection.prepareTupleQuery(queryString);
		query.setIncludeInferred(true);
		TupleQueryResult result = query.evaluate();
		while (result.hasNext()) {
			BindingSet next = result.next();
			System.err.println(next.getBinding("s").getValue());
		}*/
		
		checkClassShapeIsInserted(connection, "http://cpgu.kbpm.ru/ns/rm/cpgu#MissingClassShape", "http://cpgu.kbpm.ru/ns/rm/cpgu#MissingClass");
		
		System.out.println("Check Wrong Data With Inserted Shapes");
		queryString = String.join("\n",
	        "PREFIX cpgu: <http://cpgu.kbpm.ru/ns/rm/cpgu#>",
	        "INSERT DATA {",
	        "  cpgu:wrongInstance a cpgu:MissingClass .",
	        "}"
	    );
	    try {
	        connection.begin();
	        connection.prepareUpdate(queryString).execute();
	        connection.commit();
	        fail();
	    } catch (RepositoryException ex) {
	        assertTrue(ex.getCause() instanceof ShaclSailValidationException);
	    }
	    
	    queryString = String.join("\n", 
	        "PREFIX cpgu: <http://cpgu.kbpm.ru/ns/rm/cpgu#>",
	        "SELECT ?p ?o WHERE {",
	        "  cpgu:wrongInstance ?p ?o .",
	        "}"
	    );
	    
	    TupleQuery query = connection.prepareTupleQuery(queryString);
        query.setIncludeInferred(true);
        TupleQueryResult result = query.evaluate();
        while (result.hasNext()) {
            BindingSet next = result.next();
            System.err.println(next.getBinding("p").getValue() + " " + next.getBinding("o").getValue());
        }
		
		connection.close();
		System.out.println("End");
	}
	
	/*@Test
	public void testLoadAllAtOnce () throws IOException {
		Model data = new LinkedHashModel();
		
		data.addAll(loadAll(vocabsFiles));
		data.addAll(loadAll(shapesFiles));
		data.addAll(loadAll(usersFiles));
		data.addAll(loadAll(projectsFoldersFiles));
		data.addAll(loadAll(samplesFiles));
		
		//Sail shaclSail = createSail();
		Repository sailRepository = createRepo();//new SailRepository(shaclSail);
		sailRepository.init();
		
		RepositoryConnection connection = sailRepository.getConnection();
		connection.begin();
		connection.add(data);
		connection.commit();
		connection.close();
		System.out.println("End");
	}*/
}
