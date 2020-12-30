/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.server.bp;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PathMatcher;

import java.net.URL;
import java.util.Collection;

import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_LOCAL;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_RECURSE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class ClasspathResolver extends InternalResolver<String> {
    private static final Logger LOG = getLogger(ClasspathResolver.class);

    ClasspathResolver(final PathMatcher matcher) {
        super(matcher);
    }

    private BundleWiring bundleWiring(final Bundle bundle) {
        return bundle.adapt(BundleWiring.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ch.bechtle.osgi.springmvc.blueprint.adapter.resolver.BaseResourceAccessor
     * #listResourcePaths(java.lang.String)
     */
    @Override
    protected Collection<String> listAllResources(final Bundle bundle) {
        final BundleWiring wiring = bundleWiring(bundle);
        LOG.debug("Bundle state of {} is {}, wiring is in use: {}", bundle.getSymbolicName(), bundle.getState(), wiring.isInUse());
        return bundleWiring(bundle).listResources("/", "*", LISTRESOURCES_RECURSE);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ch.bechtle.osgi.springmvc.blueprint.adapter.resolver.BaseResourceAccessor
     * #resolveResource(java.lang.String)
     */
    @Override
    protected URL doResolveResource(final Bundle bundle, final String path) {
        return bundleWiring(bundle).getClassLoader().getResource(path);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ch.bechtle.osgi.springmvc.blueprint.adapter.resolver.BaseResourceAccessor
     * #toPath(java.lang.Object)
     */
    @Override
    protected String toPath(final String path, final String pattern) {
        return path;
    }
}
