package ru.agentlab.rdf4j.sail.shacl.sparqled.config;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

import ru.agentlab.rdf4j.sail.shacl.sparqled.SparqledShaclSail;

public class SparqledShaclSailFactory implements SailFactory {

	public static final String SAIL_TYPE = "rdf4j:SparqledShaclSail";

	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new SparqledShaclSailConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		SparqledShaclSail sail = new SparqledShaclSail();

		if (config instanceof SparqledShaclSailConfig) {
			SparqledShaclSailConfig shaclSailConfig = (SparqledShaclSailConfig) config;

			if (shaclSailConfig.isValidationEnabled()) {
				sail.enableValidation();
			} else {
				sail.disableValidation();
			}

			sail.setCacheSelectNodes(shaclSailConfig.isCacheSelectNodes());
			sail.setUndefinedTargetValidatesAllSubjects(shaclSailConfig.isUndefinedTargetValidatesAllSubjects());
			sail.setIgnoreNoShapesLoadedException(shaclSailConfig.isIgnoreNoShapesLoadedException());
			sail.setLogValidationPlans(shaclSailConfig.isLogValidationPlans());
			sail.setLogValidationViolations(shaclSailConfig.isLogValidationViolations());
			sail.setParallelValidation(shaclSailConfig.isParallelValidation());
			sail.setGlobalLogValidationExecution(shaclSailConfig.isGlobalLogValidationExecution());
			sail.setPerformanceLogging(shaclSailConfig.isPerformanceLogging());
			sail.setSerializableValidation(shaclSailConfig.isSerializableValidation());
			sail.setRdfsSubClassReasoning(shaclSailConfig.isRdfsSubClassReasoning());
			sail.setEclipseRdf4jShaclExtensions(shaclSailConfig.isEclipseRdf4jShaclExtensions());
			sail.setDashDataShapes(shaclSailConfig.isDashDataShapes());
			sail.setValidationResultsLimitTotal(shaclSailConfig.getValidationResultsLimitTotal());
			sail.setValidationResultsLimitPerConstraint(shaclSailConfig.getValidationResultsLimitPerConstraint());
		}

		return sail;

	}

}
