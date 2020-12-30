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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 *
 */
public class BlueprintServletContainerInitializer implements ServletContainerInitializer, ServletContextAttributeListener {
    private static final Logger LOG = getLogger(BlueprintServletContainerInitializer.class);

    /**
     * Attribute name to get the bundle context of the corresponding WAB through
     * {@link ServletContext#getAttribute(String)}. See OSGi Enterprise
     * specification R5 (page 457, section 128.6.1).
     */
    public static final String OSGI_BUNDLECONTEXT = "osgi-bundlecontext";

    /**
     * Init parameter name to declare a custom Blueprint/Spring context class.
     */
    public static final String BLUEPRINT_CONTEXT_CLASS = "blueprintContextClass";

    /**
     * Attribute used by Spring-Web to find an existing application context on the
     * servlet context.
     */
    public static final String BLUEPRINT_CONTEXT = "blueprintContext";

    /**
     * Attribute used by to specify the attribute-name to be used to find
     * an existing application context.
     */
    public static final String CONTEXT_ATTRIBUTE = "contextAttribute";
    public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

    static Bundle getBundle(final ServletContext context) {
        return ((BundleContext) requireNonNull(context.getAttribute(OSGI_BUNDLECONTEXT),
                () -> OSGI_BUNDLECONTEXT + " is not set as attribute on ServletContext")).getBundle();
    }
    
    WebContainer webContainer;
    HttpContext httpContext;
    public ConfigurableWebApplicationContext webContext;
    
    public BlueprintServletContainerInitializer(WebContainer webContainer, HttpContext httpContext) {
		this.webContainer = webContainer;
		this.httpContext = httpContext;
	}

    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {
        ctx.addListener(this);
        final BundleContext bundleContext = (BundleContext) ctx.getAttribute(OSGI_BUNDLECONTEXT);
        if(bundleContext != null) {
        	register(ctx, bundleContext);
        }
    }

    private ConfigurableWebApplicationContext createContext(final Bundle bundle, final String classNameOrNull)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        ConfigurableWebApplicationContext ctx;
        if (classNameOrNull == null) {
            ctx = new GenericWebApplicationContext();//XmlWebApplicationBundleContext();
        } else {
            final Class<?> cl = bundle.loadClass(classNameOrNull);
            if (!ConfigurableWebApplicationContext.class.isAssignableFrom(cl)) {
                throw new ClassCastException(format("Class %s specified by attribute %s must be assignable from %s",
                        cl.getName(), BLUEPRINT_CONTEXT_CLASS, ConfigurableWebApplicationContext.class.getName()));
            }
            ctx = (ConfigurableWebApplicationContext) cl.newInstance();
        }
        return ctx;
    }

    @Override
    public void attributeAdded(final ServletContextAttributeEvent event) {
        if (OSGI_BUNDLECONTEXT.equals(event.getName())) {
            final ServletContext sctx = event.getServletContext();
            final BundleContext bundleContext = (BundleContext) event.getValue();
            register(sctx, bundleContext);
        }
    }
    
    void register(ServletContext sctx, BundleContext bundleContext) {
    	final BlueprintApplicationContext blueprintApplicationContext = new BlueprintApplicationContext(sctx, bundleContext);
    	blueprintApplicationContext.setWebContainer(webContainer, httpContext);
       	blueprintApplicationContext.start();

        //final ClassLoader ldr = currentThread().getContextClassLoader();
        //currentThread().setContextClassLoader(new ResourceFinderClassLoader(bundleContext));
        try {
            webContext = createContext(bundleContext.getBundle(), sctx.getInitParameter(BLUEPRINT_CONTEXT_CLASS));
            webContext.setServletContext(sctx);
            //webContext.setParent(blueprintApplicationContext);
            String configLocationParam = sctx.getInitParameter(CONFIG_LOCATION_PARAM);
            if (configLocationParam != null) {
                webContext.setConfigLocation(configLocationParam);
            }

            //webContext.refresh();
            blueprintApplicationContext.setWebContext(webContext);
        } catch (final Exception e) {
            sctx.setAttribute(BLUEPRINT_CONTEXT, e);
        } finally {
            //currentThread().setContextClassLoader(ldr);
        }
    }

    @Override
    public void attributeRemoved(final ServletContextAttributeEvent event) {
        // noop
    }

    @Override
    public void attributeReplaced(final ServletContextAttributeEvent event) {
    	if (OSGI_BUNDLECONTEXT.equals(event.getName())) {
            final ServletContext sctx = event.getServletContext();
            final BundleContext bundleContext = (BundleContext) event.getValue();
            register(sctx, bundleContext);
        }
    }
}
