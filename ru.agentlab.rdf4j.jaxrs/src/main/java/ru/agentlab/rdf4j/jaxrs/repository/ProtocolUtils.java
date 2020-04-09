package ru.agentlab.rdf4j.jaxrs.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

public class ProtocolUtils {
	
	/**
	 * Obtain a new {@link RepositoryConnection} with suitable parser/writer configuration for handling the incoming
	 * HTTP request. The caller of this method is responsible for closing the connection.
	 * 
	 * @param repository the {@link Repository} for which a {@link RepositoryConnection} is to be returned
	 * @return a configured {@link RepositoryConnection}
	 */
	public static RepositoryConnection getRepositoryConnection(Repository repository) {
		RepositoryConnection conn = repository.getConnection();
		conn.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		conn.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
		return conn;
	}

}
