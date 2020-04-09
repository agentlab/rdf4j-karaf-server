package ru.agentlab.rdf4j.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = RepositoryManagerComponent.class)
public class RepositoryManagerComponent extends LocalRepositoryManager {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryManagerComponent.class);

	protected static File folder = new File("./");

	public RepositoryManagerComponent() {
		super(folder);
	}

	@Activate
	public void activate() {
		logger.info("Activate " + this.getClass().getSimpleName());
	}

	@Deactivate
	public void deactivate() {
		logger.info("Deactivate " + this.getClass().getSimpleName());
		shutDown();
	}

	public Repository getOrCreateRepository(String repId, String type, Map<String, String> templateParams) throws IOException {
		Repository repository = getRepository(repId);
		if (repository == null) {
			if (type == null)
				type = "native";
			if (templateParams == null)
				templateParams = new HashMap<>();

			ConfigTemplate ct = getConfigTemplate(type);
			templateParams.put("Repository ID", repId);
			String strConfTemplate = ct.render(templateParams);

			logger.info("UpdateRepositoryConfig with ConfigTemplate: {}", strConfTemplate);
			updateRepositoryConfig(strConfTemplate);
			repository = getRepository(repId);
		}
		return repository;
	}

	public Repository getOrCreateRepository(String repId, final InputStream ttlInput, Map<String, String> templateParams) throws IOException {
		Repository repository = getRepository(repId);
		if (repository == null) {
			if (ttlInput == null)
				return null;
			if (templateParams == null)
				templateParams = new HashMap<>();

			ConfigTemplate ct = getConfigTemplate(ttlInput);
			templateParams.put("Repository ID", repId);
			String strConfTemplate = ct.render(templateParams);

			logger.info("UpdateRepositoryConfig with ConfigTemplate: {}", strConfTemplate);
			updateRepositoryConfig(strConfTemplate);
			repository = getRepository(repId);
		}
		return repository;
	}

	public RepositoryConfig updateRepositoryConfig(final String configString) throws IOException, RDF4JException {
		final Model graph = new LinkedHashModel();
		final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, SimpleValueFactory.getInstance());
		rdfParser.setRDFHandler(new StatementCollector(graph));

		logger.debug("Parse Repository Config");
		rdfParser.parse(new StringReader(configString), RepositoryConfigSchema.NAMESPACE);

		logger.debug("Checking Repository Config");
		Resource res = Models.subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY))
				.orElseThrow(() -> new RepositoryException("could not find instance of Repository class in config"));

		logger.debug("Creating Repository Config");
		final RepositoryConfig repConfig = RepositoryConfig.create(graph, res);

		logger.debug("Validating Repository Config");
		repConfig.validate();

		logger.debug("Adding Repository Config");
		addRepositoryConfig(repConfig);

		return repConfig;
	}

	public static ConfigTemplate getConfigTemplate(final String type) throws IOException {
		try (InputStream ttlInput = RepositoryConfig.class.getResourceAsStream(type + ".ttl")) {
			return getConfigTemplate(ttlInput);
		}
	}

	public static ConfigTemplate getConfigTemplate(final InputStream ttlInput) throws UnsupportedEncodingException, IOException {
		final String template = IOUtil.readString(new InputStreamReader(ttlInput, "UTF-8"));
		return new ConfigTemplate(template);
	}
}
