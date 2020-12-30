/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.sail.shacl.sparqled;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

public class SparqledShaclSailConnection extends NotifyingSailConnectionWrapper {
    
    protected SailRepositoryConnection shapesRepoConnection;

	public SparqledShaclSailConnection(NotifyingSailConnection connection) {
		super(connection);
		try {
            shapesRepoConnection = (SailRepositoryConnection) FieldUtils.readField(connection, "shapesRepoConnection", true);
        } catch (IllegalAccessException e) {
            shapesRepoConnection = null;
            e.printStackTrace();
        }
	}

	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
		
		if (dataset == null) {
			return super.evaluate(tupleExpr, dataset, bindings, includeInferred);
		} else {
			List<IRI> graphs = new ArrayList<IRI>();
			graphs.addAll(dataset.getDefaultGraphs());
			graphs.addAll(dataset.getNamedGraphs());
			
			if (shapesRepoConnection != null && graphs.contains(RDF4J.SHACL_SHAPE_GRAPH)) {
			    CloseableIteration<? extends BindingSet, QueryEvaluationException> result = shapesRepoConnection.getSailConnection().evaluate(tupleExpr, null, bindings, includeInferred);
			    // if SPARQL Update
			    try {
                    FieldUtils.writeField(this.getWrappedConnection(), "isShapeRefreshNeeded", true, true);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
			    return result;
			} else {
				return super.evaluate(tupleExpr, dataset, bindings, includeInferred);
			}
		}
	}
}
