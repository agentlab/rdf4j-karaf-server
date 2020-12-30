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

import static org.slf4j.LoggerFactory.getLogger;
import static ru.agentlab.rdf4j.server.bp.BlueprintServletContainerInitializer.getBundle;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;

/**
 *
 */
public class XmlWebApplicationBundleContext extends AbstractRefreshableWebApplicationContext /*XmlWebApplicationContext*/ {
	private static final Logger LOG = getLogger(XmlWebApplicationBundleContext.class);
	
    private BundleResourcePatternResolver resolver;

    /**
     *
     */
    @Override
    protected ResourcePatternResolver getResourcePatternResolver() {
        if (resolver == null) {
            resolver = new BundleResourcePatternResolver(super.getResourcePatternResolver());
        }
        return resolver;
    }

    /**
     *
     */
    protected MessageSource getInternalParentMessageSource() {
        try {
            return getBeanFactory().getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
        } catch (final NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    @Override
    public void setServletContext(final ServletContext servletContext) {
        resolver.setBundle(getBundle(servletContext));
        super.setServletContext(servletContext);
    }

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		LOG.debug("loadBeanDefinitions");
	}
}
