/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.sail.shacl;

import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.eclipse.rdf4j.sail.inferencer.fc.config.DirectTypeHierarchyInferencerConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.agentlab.rdf4j.sail.shacl.sparqled.config.SparqledShaclSailConfig;
import ru.agentlab.rdf4j.sail.shacl.sparqled.config.SparqledShaclSailFactory;

public class ShaclParallelStressTest {

	@ClassRule
	public static TemporaryFolder tempDir = new TemporaryFolder();

	private static final String TEST_REPO = "testParralel";

	static Repository sailRepository;

	protected static Repository createRepo() throws IOException {
		LocalRepositoryManager manager = new LocalRepositoryManager(tempDir.newFolder("reps"));
		manager.addRepositoryConfig(new RepositoryConfig(TEST_REPO,
				new SailRepositoryConfig(new ShaclSailConfig(new DirectTypeHierarchyInferencerConfig(
						new SchemaCachingRDFSInferencerConfig(new NativeStoreConfig("spoc,posc")))))));
		return manager.getRepository(TEST_REPO);
	}

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
	}

	/*
	 * org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException: Failed SHACL
	 * validation or deadlock
	 */
	@Test
	public void parallelLoadShapes() throws IOException, InterruptedException {
		List<FileUploadConfig> files = new LinkedList<FileUploadConfig>();
		Collections.addAll(files, ShaclValidatorTest.vocabsFiles);
		Collections.addAll(files, ShaclValidatorTest.shapesFiles);
		Collections.addAll(files, ShaclValidatorTest.usersFiles);
		Collections.addAll(files, ShaclValidatorTest.projectsFoldersFiles);
		Collections.addAll(files, ShaclValidatorTest.samplesFiles);

		ExecutorService service = Executors.newFixedThreadPool(files.size());
		CountDownLatch latch = new CountDownLatch(files.size());
		for (int i = 0; i < files.size(); i++) {
			final FileUploadConfig file = files.get(i);
			service.execute(() -> {
				System.out.println("Thread starts");
				RepositoryConnection connection = sailRepository.getConnection();
				try {
					Model model = Rio.parse(new FileInputStream(file.file), file.baseURI, RDFFormat.TURTLE);
					connection.begin();
					connection.add(model, file.graph);
					connection.commit();
				} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
					e.printStackTrace();
					fail();
				} finally {
					if (connection != null)
						connection.close();
					latch.countDown();
					System.out.println("Thread ends");
				}
			});
		}
		latch.await();
		System.out.println("END");
	}

	/*
	 * works in Eclipse did not work in CLI
	 */
	@Test
	public void parallelLoadDataAndBulkShapes() throws IOException, InterruptedException {
		List<FileUploadConfig> files = new LinkedList<FileUploadConfig>();
		Collections.addAll(files, ShaclValidatorTest.vocabsFiles);
		// Collections.addAll(files, ShaclValidatorTest.shapesFiles);
		Collections.addAll(files, new FileUploadConfig(null, null, null));
		Collections.addAll(files, ShaclValidatorTest.usersFiles);
		Collections.addAll(files, ShaclValidatorTest.projectsFoldersFiles);
		Collections.addAll(files, ShaclValidatorTest.samplesFiles);

		ExecutorService service = Executors.newFixedThreadPool(files.size());
		CountDownLatch latch = new CountDownLatch(files.size());
		for (int i = 0; i < files.size(); i++) {
			final FileUploadConfig file = files.get(i);
			service.execute(() -> {
				System.out.println("Thread starts");
				RepositoryConnection connection = sailRepository.getConnection();
				try {
					Model model;
					if (file.file == null) {
						model = ShaclValidatorTest.loadAll(ShaclValidatorTest.shapesFiles);
					} else {
						String filePath = Paths.get(file.file).toAbsolutePath().toString();
						System.out.println("filePath=" + filePath);
						model = Rio.parse(new FileInputStream(filePath), file.baseURI, RDFFormat.TURTLE);
					}
					// connection.begin();
					connection.add(model, file.graph);
					// connection.commit();
				} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
					e.printStackTrace();
					fail();
				} finally {
					if (connection != null)
						connection.close();
					latch.countDown();
					System.out.println("Thread ends");
				}
			});
		}
		latch.await();

		RepositoryConnection connection = sailRepository.getConnection();
		String queryString = String.join("\n", 
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
			"PREFIX dcterms: <http://purl.org/dc/terms/>",
			"PREFIX rm: <http://cpgu.kbpm.ru/ns/rm/rdf#>",
			"PREFIX folders: <http://cpgu.kbpm.ru/ns/rm/folders#>",
			"PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>",
			"INSERT DATA {",
			"  <http://cpgu.kbpm.ru/ns/rm/rdf#_6yT5Ka2upncrlFJrfzMREt> rdf:type <http://cpgu.kbpm.ru/ns/rm/cpgu#Группировка>;",
			"    dcterms:title \"Требование 2m7egLn2Sdr4rZLRcLdf0P\";",
			"    dcterms:description \"Описание 2m7egLn2Sdr4rZLRcLdf0P\";",
			"    rm:assetFolder folders:folder1;",
			"    rm:artifactFormat <http://cpgu.kbpm.ru/ns/rm/user-types#_YwcOsRmREemK5LEaKhoOow_Text>;",
			"    dcterms:identifier 12345 .",
			"}"
		);
		System.out.println("Prepare Query");
		Update u = connection.prepareUpdate(QueryLanguage.SPARQL, queryString);
		System.out.println("Execute Query");
		u.execute();

		System.out.println("Close conn");
		if (connection != null)
			connection.close();
		System.out.println("END");
	}
}
