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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PathMatcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
abstract class InternalResolver<T> {
    private static final Logger LOG = getLogger(InternalResolver.class);
    private final PathMatcher matcher;

    InternalResolver(final PathMatcher matcher) {
        this.matcher = matcher;
    }

    abstract Collection<T> listAllResources(Bundle bundle);

    abstract URL doResolveResource(final Bundle bundle, String path);

    final URL resolveResource(final Bundle bundle, final String path) {
        final URL resolvedResource = doResolveResource(bundle, path);

        // This should never happen because the resource path has been received
        // through a specified mechanism which must insure that a resource
        // exists.
        if (resolvedResource == null) {
            throw new IllegalStateException(path + " could not be resolved to an URL object!");
        }

        return resolvedResource;
    }

    /**
     *
     */
    abstract String toPath(T path, String pattern);

    final Collection<URL> resolveResources(final Bundle bundle, final String pattern)
            throws IOException {
        // Create the result set and list recursively all resources contained by
        // the directory specified.
        final Collection<URL> foundResources = new LinkedList<>();
        final Collection<T> resourcePaths = listAllResources(bundle);

        LOG.debug("Following resources listed for {} before filtering: {}",
                bundle.getSymbolicName(),
                resourcePaths);

        if (resourcePaths != null && !resourcePaths.isEmpty()) {
            for (final T resourcePath : resourcePaths) {
                // Check whether we need to resolve and include the current path
                // into the search result. Ignore directories!
                final String resourcePathAsString = toPath(resourcePath, pattern);
                if (matcher.match(pattern, resourcePathAsString) && !resourcePathAsString.endsWith("/")) {
                    foundResources.add(resolveResource(bundle, resourcePathAsString));
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignored {} from {} because it does not match pattern {}",
                            resourcePathAsString, bundle.getSymbolicName(), pattern);
                }
            }
        }

        return foundResources;
    }
}
