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
import org.springframework.util.PathMatcher;

import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import static java.util.Collections.emptyList;
import static java.util.Collections.list;

/**
 *
 */
class BundleSpaceResolver extends InternalResolver<URL> {

    /**
     *
     */
    BundleSpaceResolver(final PathMatcher matcher) {
        super(matcher);
    }

    /*
     * (non-Javadoc)
     *
     * @see ch.bechtle.osgi.springmvc.blueprint.adapter.BaseResourceAccessor#
     * listAllResources(org.osgi.framework.Bundle, java.lang.String)
     */
    @Override
    protected Collection<URL> listAllResources(final Bundle bundle) {
        final Enumeration<URL> urls = bundle.findEntries("/", "*", true);
        final Collection<URL> resources;

        if (urls == null) {
            resources = emptyList();
        } else {
            resources = list(urls);
        }

        return resources;
    }

    /*
     * (non-Javadoc)
     *
     * @see ch.bechtle.osgi.springmvc.blueprint.adapter.BaseResourceAccessor#
     * doResolveResource(org.osgi.framework.Bundle, java.lang.String)
     */
    @Override
    protected URL doResolveResource(final Bundle bundle, final String path) {
        return bundle.getEntry(path);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ch.bechtle.osgi.springmvc.blueprint.adapter.resolver.BaseResourceAccessor
     * #toPath(java.lang.Object)
     */
    @Override
    protected String toPath(final URL url, final String pattern) {
        String path = url.getPath();

        // Remove leading slash
        if (path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }
}
