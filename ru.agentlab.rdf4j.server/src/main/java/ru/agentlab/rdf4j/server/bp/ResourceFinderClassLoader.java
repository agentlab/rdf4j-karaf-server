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
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static java.util.Collections.enumeration;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ResourceFinderClassLoader extends ClassLoader {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ResourceFinderClassLoader.class);
    private final BundleContext context;

    public ResourceFinderClassLoader(final BundleContext context) {
        super(getBundleClassLoader(context.getBundle()));
        this.context = context;
    }

    static ClassLoader getBundleClassLoader(final Bundle bundle) {
        return bundle.adapt(BundleWiring.class).getClassLoader();
    }

    @Override
    protected Enumeration<URL> findResources(final String name) throws IOException {
        final List<URL> resources = new LinkedList<>();
        for (final Bundle b : context.getBundles()) {
            final ClassLoader cl = getBundleClassLoader(b);
            if (cl != null) {
                final Enumeration<URL> e = cl.getResources(name);
                while (e.hasMoreElements()) {
                    resources.add(e.nextElement());
                }
            }
        }
        return enumeration(resources);
    }

  @Override
  protected URL findResource(String name){
    for (final Bundle b : context.getBundles()) {
        final ClassLoader cl = getBundleClassLoader(b);
        if (cl != null) {
            URL url = cl.getResource(name);
            if(url != null){
                return url;
            }
        }
    }
    return null;
  }
    
    
}
