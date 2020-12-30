/********************************************************************************
 * Copyright (c) 2020 Agentlab and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package ru.agentlab.rdf4j.server;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

/*@Component(
	service = Filter.class,
	//immediate = true,
	//scope = ServiceScope.PROTOTYPE,
	property = {
		//"osgi.http.whiteboard.filter.pattern=/rdf4j-server/*",
		//"osgi.http.whiteboard.filter.pattern=/rdf4j-server/repositories/*",
		//"osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=org.osgi.service.http)"
		//"osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=org.eclipse.rdf4j.http.server.bp.BlueprintApplicationContext$AppServletContext)"
		//"osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=*)"
		"osgi.http.whiteboard.filter.servlet=org.springframework.web.servlet.DispatcherServlet"
	}
)*/
public class CorsFilter implements Filter {
	private static final Logger LOG = getLogger(CorsFilter.class);
    protected static final String ALLOW_ORIGIN = getEnv("ALLOW_ORIGIN", "*");
    protected static final String ALLOW_HEADERS = "origin, content-type, accept, authorization, user-agent, cookie";
    protected static final String EXPOSE_HEADERS = "Set-Cookie, Cache-Control, Content-Language, Content-Length, Content-Type, Expires, Last-Modified, Pragma";
    protected static final int MAX_AGE = 42 * 60 * 60;
    
    public CorsFilter() {
    	LOG.info("CorsFilter Create");
    }

    private static String getEnv(String key, String def) {
        String value = System.getenv(key);
        return value != null ? value : def;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	LOG.info("CorsFilter Init");
    }

    @Override
    public void doFilter(ServletRequest servletrequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletrequest;
        
        LOG.debug("CORSFilter HTTP Request: {} {}", request.getMethod(), request.getRequestURI());
 
        // Authorize (allow) all domains to consume the content
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Credentials", "true");
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Origin", ALLOW_ORIGIN);
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Methods","GET, OPTIONS, HEAD, PUT, POST, DELETE");
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Expose-Headers", EXPOSE_HEADERS);
        ((HttpServletResponse) servletResponse).addHeader("Access-Control-Max-Age", new Integer(MAX_AGE).toString());
 
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
 
        // For HTTP OPTIONS verb/method reply with ACCEPTED status code -- per CORS handshake
        if (request.getMethod().equals("OPTIONS")) {
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
        }
        // pass the request along the filter chain
        chain.doFilter(request, servletResponse);        
    }

    @Override
    public void destroy() {
    	LOG.info("CorsFilter Stop");
    }
}
